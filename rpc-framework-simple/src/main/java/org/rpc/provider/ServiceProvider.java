package org.rpc.provider;


import org.rpc.config.RpcServiceConfig;

/**
存储和提供服务对象
 */
public interface ServiceProvider {

    /**
     * @param rpcServiceConfig RPC相关的属性
     */
    void addService(RpcServiceConfig rpcServiceConfig);

    /**
     * @param rpcServiceName RPC服务名称
     * @return service object
     */
    Object getService(String rpcServiceName);

    /**
     * @param rpcServiceConfig RPC相关的属性
     */
    void publishService(RpcServiceConfig rpcServiceConfig);

}
