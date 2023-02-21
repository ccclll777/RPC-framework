package org.rpc.loadbalance.loadbalancer;



import org.rpc.loadbalance.AbstractLoadBalance;
import org.rpc.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * 随机策略，随机选一个服务
 */
public class RandomLoadBalance extends AbstractLoadBalance {
    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
