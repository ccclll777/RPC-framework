package org.rpc.provider.impl;

import lombok.extern.slf4j.Slf4j;
import org.rpc.config.RpcServiceConfig;
import org.rpc.enums.RpcErrorMessageEnum;
import org.rpc.exception.RpcException;
import org.rpc.extension.ExtensionLoader;
import org.rpc.provider.ServiceProvider;
import org.rpc.registry.ServiceRegistry;
import org.rpc.remoting.transport.netty.server.NettyRpcServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {
    /*
         存储 rpc ServiceName到 service Object的映射
         rpcServiceName的形式为 interface name + version + group
     */
    private final Map<String, Object> serviceMap;
    /*
        已经完成注册的服务
     */
    private final Set<String> registeredService;
    /*
    注册服务的注册器 目前是基于Zookeeper做的注册中心
     */
    private final ServiceRegistry serviceRegistry;
    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
    }
    @Override
    /*
    添加服务
     */
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (registeredService.contains(registeredService)) {
            return;
        }
        registeredService.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces:{}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }
    /*
    根据serviceName获取服务的实例
     */
    @Override
    public  Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }
    /*
    发布服务
     */
    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }

    }
}
