package org.rpc.register.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.rpc.register.ServiceRegistry;
import org.rpc.register.zookeeper.utils.CuratorUtils;

import java.net.InetSocketAddress;
/*
    基于zookeeper的服务注册
 */
public class ZkServiceRegistryImpl implements ServiceRegistry {

    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}
