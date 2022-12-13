package org.rpc.remoting.handler;

import lombok.extern.slf4j.Slf4j;
import org.rpc.exception.RpcException;
import org.rpc.factory.SingletonFactory;
import org.rpc.provider.ServiceProvider;
import org.rpc.provider.impl.ZkServiceProviderImpl;
import org.rpc.remoting.dto.RpcRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class RpcRequestHandler {
    /*
    service Provider 就是基于zookeeper的服务提供者
    */
    private final ServiceProvider serviceProvider;
    public RpcRequestHandler() {
        serviceProvider   = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }
    /*
        处理RPC请求，调用对应的方法，并且返回结果
     */
    public Object handle(RpcRequest rpcRequest) {
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }
    /*
        获取方法的执行结果
     */
    public Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            //通过反射，从service中获取到要执行的方法
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service:[{}] successful invoke method:[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());

        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
