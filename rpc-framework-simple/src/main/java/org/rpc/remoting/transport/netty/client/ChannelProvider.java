package org.rpc.remoting.transport.netty.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/*
    存储和获取Channel的类 ，为每个服务的地址对应一个channel处理网络的操作
    Channel是Netty中进行对网络操作抽象类，通过 Channel 我们可以进行 I/O 操作。
 */
@Slf4j
public class ChannelProvider {
    private final Map<String, Channel> channelMap;
    public ChannelProvider() {
        channelMap = new HashMap<>();
    }
    /*
        根据socket的地址获取对应的Channel
     */
    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        //判断释放存在对应连接的地址
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            //如果存在连接，则判断连接是否可用
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(key);
            }
        }
        return null;
    }
    /*
    为某个请求的连接设置一个channel
     */
    public void set(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key, channel);
    }
    /*
        为某个连接移除了channel
     */
    public void remove(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        channelMap.remove(key);
        log.info("Channel map size :[{}]", channelMap.size());
    }
}
