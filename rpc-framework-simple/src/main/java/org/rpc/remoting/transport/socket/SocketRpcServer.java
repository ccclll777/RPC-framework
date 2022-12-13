package org.rpc.remoting.transport.socket;

import lombok.extern.slf4j.Slf4j;
import org.rpc.config.CustomShutdownHook;
import org.rpc.config.RpcServiceConfig;
import org.rpc.factory.SingletonFactory;
import org.rpc.provider.ServiceProvider;
import org.rpc.utils.threadpool.ThreadPoolFactoryUtil;
import org.rpc.provider.impl.ZkServiceProviderImpl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import static org.rpc.remoting.transport.netty.server.NettyRpcServer.PORT;
@Slf4j
public class SocketRpcServer {
    private final ExecutorService threadPool;
    private final ServiceProvider serviceProvider; //提供服务的服务程序
    public SocketRpcServer() {
        //创建服务提供程序的线程池
        threadPool = ThreadPoolFactoryUtil.createCustomThreadPoolIfAbsent("socket-server-rpc-pool");
        //单例工厂 提供基于zk的服务提供者
        //单例工厂中有一个Map，如果已经创建过实例，则会直接返回，
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }
    public void registerService(RpcServiceConfig rpcServiceConfig) {
        //通过zk发布服务
        serviceProvider.publishService(rpcServiceConfig);
    }
    /*
        启动服务
     */
    public void start() {
        try (ServerSocket server = new ServerSocket()) {
            //服务器提供服务的流程
            String host = InetAddress.getLocalHost().getHostAddress();
            server.bind(new InetSocketAddress(host, PORT)); //先绑定端口
            CustomShutdownHook.getCustomShutdownHook().clearAll();
            Socket socket;
            while ((socket = server.accept()) != null) {
                log.info("client connected [{}]", socket.getInetAddress());
                threadPool.execute(new SocketRpcRequestHandlerRunnable(socket));
            }
            threadPool.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
