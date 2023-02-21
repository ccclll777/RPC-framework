package org.rpc.remoting.transport.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rpc.config.CustomShutdownHook;
import org.rpc.config.RpcServiceConfig;
import org.rpc.factory.SingletonFactory;
import org.rpc.provider.ServiceProvider;
import org.rpc.provider.impl.ZkServiceProviderImpl;
import org.rpc.remoting.transport.netty.codec.RpcMessageDecoder;
import org.rpc.remoting.transport.netty.codec.RpcMessageEncoder;
import org.rpc.utils.RuntimeUtil;
import org.rpc.utils.threadpool.ThreadPoolFactoryUtil;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NettyRpcServer {
    public static final int PORT = 19998;
    /*
        单例模式，基于zk的服务发现和服务提供
     */
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    /*
        将服务发布到zk中
     */
    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }
    @SneakyThrows  //为我们的代码生成一个try...catch块，并把异常向上抛出来
    public void start() {
        CustomShutdownHook.getCustomShutdownHook().clearAll(); //停止之前在zk中注册的所有服务
        String host = InetAddress.getLocalHost().getHostAddress();
        //多线程模型。一个线程负责监听客户端连接 多个线程用于接受数据
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); //一个bossGroup负责接受连接，对应1个线程
        EventLoopGroup workerGroup = new NioEventLoopGroup(); //workerGroup负责进行I/O，对应多个EventLoop，每个EventLoop对应一个线程
        //服务端执行事件的Handler 穿件多个线程 数量为2*cpu个数
        //Netty提供的 非I/O线程池——DefaultEventExecutorGroup
        //内部聚合了Java的Thread，但没有I/O多路复用器，侧重于处理耗时业务逻辑（非I/O操作）
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpus() * 2,
                //非守护线程
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 30 秒之内没有收到客户端请求的话就关闭连接
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            p.addLast(new RpcMessageEncoder()); //先解码
                            p.addLast(new RpcMessageDecoder()); //再编码
                            p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            // 绑定端口，同步等待绑定成功
            ChannelFuture f = b.bind(host, PORT).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("occur exception when start server:", e);
        } finally {
            //try cache 要在final 中关闭端口
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }
}
