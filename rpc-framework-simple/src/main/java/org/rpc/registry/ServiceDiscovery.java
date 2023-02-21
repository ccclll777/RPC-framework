package org.rpc.registry;

import org.rpc.extension.SPI;
import org.rpc.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

/**
 * service discovery
 *
 * @author shuang.kou
 * @createTime 2020年06月01日 15:16:00
 */
@SPI
public interface ServiceDiscovery {
    /**
     * 通过rpcServiceName查找对应的服务  返回服务对应的socket地址 ip+port
     *
     * @param rpcRequest rpc service pojo
     * @return service address
     */
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
