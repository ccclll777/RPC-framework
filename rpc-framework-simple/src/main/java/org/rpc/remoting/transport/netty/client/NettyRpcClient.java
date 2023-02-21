package org.rpc.remoting.transport.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rpc.enums.CompressTypeEnum;
import org.rpc.enums.SerializationTypeEnum;
import org.rpc.extension.ExtensionLoader;
import org.rpc.factory.SingletonFactory;
import org.rpc.registry.ServiceDiscovery;
import org.rpc.remoting.constants.RpcConstants;
import org.rpc.remoting.dto.RpcMessage;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.remoting.dto.RpcResponse;
import org.rpc.remoting.transport.RpcRequestTransport;
import org.rpc.remoting.transport.netty.codec.RpcMessageDecoder;
import org.rpc.remoting.transport.netty.codec.RpcMessageEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/*
    初始化并且关闭Bootstrap的对象
 */
@Slf4j
public class NettyRpcClient  implements RpcRequestTransport {
    private final ServiceDiscovery serviceDiscovery; //服务发现
    private final UnprocessedRequests unprocessedRequests; //未处理的请求列表
    private final ChannelProvider channelProvider; //channel的创建和存储
    private final Bootstrap bootstrap; //启动
    private final EventLoopGroup eventLoopGroup; //EventLoopGroup

    public NettyRpcClient() {
        //初始化EventLoopGroup、Bootstrap等资源
        //EventLoopGroup 包含多个 EventLoop（每一个 EventLoop 通常内部包含一个线程）
        eventLoopGroup = new NioEventLoopGroup();
        //引导类
        bootstrap = new Bootstrap();
        //给引导类配置了线程组
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //连接的超时时间 如果超过这个时间 则建立连接失败
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        //业务的处理逻辑
                        ChannelPipeline p = ch.pipeline();
                        //如果15s没有数据发送到服务器，则发送心跳请求
                        p.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        p.addLast(new RpcMessageEncoder()); //为什么先encoder啊
                        p.addLast(new RpcMessageDecoder());
                        p.addLast(new NettyRpcClientHandler());
                    }
                });
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        //CompletableFuture ： Java中进行异步编程
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        //建立连接
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                //连接成功
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }
    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        //构建返回值
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        //获取提供server的地址 通过负载均衡从zk中选择并且返回。
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            //放置未处理的请求
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                    .codec(SerializationTypeEnum.HESSIAN.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE).build();
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    future.channel().close();
                    //完成时发生异常
                    resultFuture.completeExceptionally(future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }
    /**
     * 获取与服务地址相关的Channel
     * @param inetSocketAddress
     * @return
     */
    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        //如果没有对应的连接，则建立连接，如果有对应的连接，则直接从channelProvider取得
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }
    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
