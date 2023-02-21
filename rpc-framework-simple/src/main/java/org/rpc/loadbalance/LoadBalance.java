package org.rpc.loadbalance;

import org.rpc.extension.SPI;
import org.rpc.remoting.dto.RpcRequest;

import java.util.List;

/**
 * 负载均衡策略的接口
 */
@SPI
public interface LoadBalance {
    /**
     * 从现在提供服务的地址中选一个
     *
     * @param serviceUrlList Service address list
     * @param rpcRequest
     * @return target service address
     */
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
