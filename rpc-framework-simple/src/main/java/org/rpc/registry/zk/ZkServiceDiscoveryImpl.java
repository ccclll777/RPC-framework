

package org.rpc.registry.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.rpc.enums.RpcErrorMessageEnum;
import org.rpc.exception.RpcException;
import org.rpc.extension.ExtensionLoader;
import org.rpc.loadbalance.LoadBalance;
import org.rpc.registry.ServiceDiscovery;
import org.rpc.registry.zk.utils.CuratorUtils;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.utils.CollectionUtil;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;
    public ZkServiceDiscoveryImpl() {
        //获取loadBalance的实例，如果没有 则会创建，如果有会直接返回
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");

    }
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        //获取到zookeeper连接的客户端
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        //获取当前rpcRequest对应的提供服务的服务端，这是通过zookeeper存储的，在zookeeper的子节点中
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        //注册中心没有注册过当前请求对应的服务
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // 通过负载均衡器，从服务列表中找到一个对应的服务
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":"); //获取到提供服务的ip+port
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        //返回socket连接的关键字
        return new InetSocketAddress(host, port);
    }
}
