package org.rpc.remoting.transport.netty.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.rpc.enums.CompressTypeEnum;
import org.rpc.enums.RpcResponseCodeEnum;
import org.rpc.enums.SerializationTypeEnum;
import org.rpc.factory.SingletonFactory;
import org.rpc.remoting.constants.RpcConstants;
import org.rpc.remoting.dto.RpcMessage;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.remoting.dto.RpcResponse;
import org.rpc.remoting.handler.RpcRequestHandler;

/*
    继承自ChannelInboundHandlerAdapter 不需要考虑bytebuf的释放
   {@link SimpleChannelInboundHandler} 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。详见《Netty进阶之路 跟着案例学 Netty》
 */
@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {
    private final RpcRequestHandler rpcRequestHandler;
    public NettyRpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof RpcMessage) {
                //如果是服务端的消息 则开始处理
                log.info("server receive msg: [{}] ", msg);
                byte messageType = ((RpcMessage) msg).getMessageType(); //消息类型
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.HESSIAN.getCode()); //序列化类型
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode()); //编码类型
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    //如果当前消息是心跳请求包，则回复Pong，表示正常
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    //如果是正常的消息 则解码请求
                    RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                    // 执行客户端需要执行的方法，并相应，可以请求的方法通过serviceName找到对应的服务，然后执行
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info(String.format("server get result: %s", result.toString()));
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE); //此时的消息类型位回应
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        //如果正常，则可以正常的执行
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        rpcMessage.setData(rpcResponse);
                    } else {
                        //响应失败
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        rpcMessage.setData(rpcResponse);
                        log.error("not writable now, message dropped");
                    }
                }
                //写入消息并刷新，并且添加监听器看是否正常执行
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            //确保释放Byte Buf，否则可能会出现内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }

    /*
    用户事件的回调
    如果空闲事件触发，则关闭链接
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        //IdleStateHandler 当连接空闲时间太长时，将会触发一个 IdleStateEvent 事件。
        // 然后，你可以通过在你的 ChannelInboundHandler 中重写 userEventTriggered()方法来处理该 IdleStateEvent 事件。
        if (event instanceof IdleStateEvent) {
            //空闲则关闭
            IdleState state = ((IdleStateEvent) event).state();
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen, so close the connection");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, event);
        }
    }
    /*
    如果捕获到异常，则获取异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
