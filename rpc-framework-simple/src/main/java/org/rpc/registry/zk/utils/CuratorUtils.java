package org.rpc.registry.zk.utils;

import org.rpc.enums.RpcConfigEnum;
import org.rpc.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Curator(zookeeper client) utils
 *
 */
@Slf4j
public final class CuratorUtils {

    private static final int BASE_SLEEP_TIME = 1000; //基本睡眠时间
    private static final int MAX_RETRIES = 3; //最大重试次数
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc"; //寄存器的根目录
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>(); //服务地址映射
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet(); //注册服务名称的set
    private static CuratorFramework zkClient; //自动化连接管理zookeeper的类
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181"; //地址

    private CuratorUtils() {
    }

    /**
     * 创建持久（PERSISTENT）节点：一旦创建就一直存在即使 ZooKeeper 集群宕机，直到将其删除。
     *
     * @param path node path
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            //每一个CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString()组成一个path，作为一个节点
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                //eg: /my-rpc/github.javaguide.HelloService/127.0.0.1:9999
                //创建永久节点
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * zookeeper子节点
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version1
     * @return All child nodes under the specified node 获取指定节点下的所有节点
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        //如果SERVICE_ADDRESS_MAP存储了这个节点，则直接返回就行了
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        //否则要从zookeeper中取出这个节点，然后存到SERVICE_ADDRESS_MAP里面 SERVICE_ADDRESS_MAP存储了rpcServiceName 和地址的映射
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    /**
     * 清空注册信息
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        //用语法糖stream的遍历方式
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }
    /*
        获取zookeeper的客户端
    */
    public static CuratorFramework getZkClient() {
        // 检查是否注册了地址
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        //zookeeper的地址
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;
        // 如果zkClient已经启动 则直接返回
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        // 重试策略。重试 3 次，并会增加重试之间的休眠时间。
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder()
                // 可以连接一个zookeeper的service 列表
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        try {
            // 等待30s 直到连接到zookeeper 否则返回
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;
    }

    /**
     * 注册Watcher 侦听对应节点的更改
     * 子节点的注册更新
     * Zookeeper的watcher机制，让其他不是master的节点监听节点的状态
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version
     */
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        //service的地址信息
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        //获取当前servicePath的子节点
        //，Curator引入了Cache的概念用来实现对ZooKeeper服务器端进行事件监听。
        //PathChildrenCache是用来监听指定节点 的子节点变化情况
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);

        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            //将子节点的信息加入rpcServiceName和地址的映射中
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        };
        //监听子节点的变化情况
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

}
