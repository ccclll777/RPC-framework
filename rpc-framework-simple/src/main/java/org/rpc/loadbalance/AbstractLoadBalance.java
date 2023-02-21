package org.rpc.loadbalance;

import org.rpc.remoting.dto.RpcRequest;
import org.rpc.utils.CollectionUtil;

import java.util.List;

/**
 * 负载均衡策略的抽象类
 *
 * @author shuang.kou
 * @createTime 2020年06月21日 07:44:00
 */
public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest) {
        /*
        根据多个提供服务的地址选择一个
         */
        if (CollectionUtil.isEmpty(serviceAddresses)) {
            return null;
        }
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }
        return doSelect(serviceAddresses, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);

}
