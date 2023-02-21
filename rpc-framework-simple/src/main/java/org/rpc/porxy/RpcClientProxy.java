package org.rpc.porxy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rpc.config.RpcServiceConfig;
import org.rpc.enums.RpcResponseCodeEnum;
import org.rpc.exception.RpcException;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.remoting.dto.RpcResponse;
import org.rpc.remoting.transport.RpcRequestTransport;
import org.rpc.remoting.transport.netty.client.NettyRpcClient;
import org.rpc.remoting.transport.socket.SocketRpcClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.rpc.enums.RpcErrorMessageEnum.*;

/*
 *动态代理类。
 *当动态代理对象调用方法时，它实际上调用了以下Invoke方法。
 *正是因为有了动态代理，客户端调用的远程方法才像调用了本地方法(中间进程被屏蔽)
 */
@Slf4j
public class RpcClientProxy implements InvocationHandler {
    private static final String INTERFACE_NAME = "interfaceName";
    /*
    动态代理将客户端的请求通过网络发送给服务端，这里 提供Socket方式和Netty两种实现
     */
    /*
    这是经过包装后的RPC的请求
     */
    private final RpcRequestTransport rpcRequestTransport;
    private final RpcServiceConfig rpcServiceConfig;
    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig rpcServiceConfig) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = rpcServiceConfig;
    }
    /*
        获取代理的实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    /**
     *此方法实际上是在使用代理对象调用方法时调用的。
     * Proxy对象是您通过getProxy方法获取的对象。
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        log.info("invoked method: [{}]", method.getName());
        //构建RPC请求
        RpcRequest rpcRequest = RpcRequest.builder().methodName(method.getName())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .group(rpcServiceConfig.getGroup())
                .version(rpcServiceConfig.getVersion())
                .build();
        RpcResponse<Object> rpcResponse = null;
        //通过不同的传输组建进行传输
        if (rpcRequestTransport instanceof NettyRpcClient) {
            //发送了RPC的请求，等待响应
            CompletableFuture<RpcResponse<Object>> completableFuture = (CompletableFuture<RpcResponse<Object>>) rpcRequestTransport.sendRpcRequest(rpcRequest);
            rpcResponse = completableFuture.get();
        }
        if (rpcRequestTransport instanceof SocketRpcClient) {
            rpcResponse = (RpcResponse<Object>) rpcRequestTransport.sendRpcRequest(rpcRequest);
        }
        //检查结果是否存在问题
        this.check(rpcResponse, rpcRequest);
        return rpcResponse.getData();
    }
    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
