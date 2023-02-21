package org.rpc.registry;

import org.rpc.extension.SPI;

import java.net.InetSocketAddress;

/**
 * service registration
 *
 * @author shuang.kou
 * @createTime 2020年05月13日 08:39:00
 */
@SPI
public interface ServiceRegistry {
    /**
     * register service
     *
     * @param rpcServiceName    服务名称
     * @param inetSocketAddress 服务地址
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);

}
