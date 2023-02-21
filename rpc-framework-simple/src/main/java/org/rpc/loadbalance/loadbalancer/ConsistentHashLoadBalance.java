package org.rpc.loadbalance.loadbalancer;

import org.rpc.loadbalance.AbstractLoadBalance;
import org.rpc.remoting.dto.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    /*
    不再将真实节点映射到哈希环上，而是将虚拟节点映射到哈希环上，并将虚拟节点映射到实际节点
    节点数量多了后，节点在哈希环上的分布就相对均匀了
     Nginx 的一致性哈希算法，每个权重为 1 的真实节点就含有160 个虚拟节点。
     */
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();
    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        int identityHashCode = System.identityHashCode(serviceAddresses); //求出对象唯一的hash码
        String rpcServiceName = rpcRequest.getRpcServiceName(); //根据服务找到对应的ServiceName
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        if (selector == null || selector.identityHashCode != identityHashCode) {
            //检查更新
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddresses, 160, identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        //选择的key是由rpcServiceName +参数组成的
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }
    static class ConsistentHashSelector {
        private final TreeMap<Long, String> virtualInvokers; //虚拟节点
        private final int identityHashCode;
        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;
            for (String invoker : invokers) {
                //replicaNumber 真实节点的副本数量？
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker + i); //先对节点求m
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h); //根据md5值和h计算一个hash值
                        virtualInvokers.put(m, invoker); //虚拟节点对应的真实调用者
                    }
                }
            }
        }
        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            return md.digest();
        }
        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 | (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }
        public  String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }
        public String selectForKey(long hashCode) {
            //在环上找到比当前节点hashCode大或者相等的节点 所以要用TreeMap
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();

            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }
            //找到第一个
            return entry.getValue();
        }
    }
}
