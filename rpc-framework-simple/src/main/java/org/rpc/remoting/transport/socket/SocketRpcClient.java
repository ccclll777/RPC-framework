package org.rpc.remoting.transport.socket;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rpc.exception.RpcException;
import org.rpc.extension.ExtensionLoader;
import org.rpc.registry.ServiceDiscovery;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.remoting.transport.RpcRequestTransport;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/*
    基于Socket想服务提供程序传输RpcRequest
 */
@AllArgsConstructor
@Slf4j
public class SocketRpcClient  implements RpcRequestTransport {
    private final ServiceDiscovery serviceDiscovery; //服务发现
    /*
    基于zk的服务发现
     */
    public SocketRpcClient() {
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
    }
    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        //通过rpcRequest从zookeeper找到提供服务的server ip+port
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        //try with resource的形式
        try (Socket socket = new Socket()) {
            socket.connect(inetSocketAddress); //和服务建立socker连接
            //代表对象输出流，它的writeObject(Object obj)方法可对参数指定的obj对象进行序列化，把得到的字节序列写到一个目标输出流中。
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(rpcRequest); //通过对象输出流像服务器发送消息
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // 从输入流中读取 Rpc Response 拿到服务端返回的处理结果
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RpcException("调用服务失败:", e);
        }
    }
}
