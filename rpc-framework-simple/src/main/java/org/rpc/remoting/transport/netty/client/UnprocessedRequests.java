package org.rpc.remoting.transport.netty.client;

import org.rpc.remoting.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/*
    服务器未处理的请求
 */
public class UnprocessedRequests {
    //到等待时间而未进行处理的请求
    //CompletableFuture主要是用于异步调用，内部封装了线程池，可以将请求或者处理过程，进行异步处理。
    private static final Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    /*
        将未处理的请求的requestId和对应的相应存起来
     */
    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }
    public void complete(RpcResponse<Object> rpcResponse) {
        //当前rpcResponse对应的未处理的请求
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            //表示这些请求已经处理完了
            //future所代表的线程已经完成
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
