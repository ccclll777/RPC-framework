package org.rpc.serialize.kyro;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.rpc.exception.SerializeException;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.remoting.dto.RpcResponse;
import org.rpc.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo序列化类，Kryo序列化效率很高，但只兼容Java语言
 *
 */
@Slf4j
public class KryoSerializer implements Serializer {

    /**
     * https://juejin.cn/post/6993647089431347237
     * 因为Kryo线程不安全，所以用ThreadLocal + Kryo 解决线程不安全
     * ThreadLocal 是一种典型的牺牲空间来换取并发安全的方式，它会为每个线程都单独创建本线程专用的 kryo 对象。
     * 对于每条线程的每个 kryo 对象来说，都是顺序执行的，因此天然避免了并发安全问题。
     */
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        /*
        Kryo 为了提供性能和减小序列化结果体积，提供注册的序列化对象类的方式。在注册时，会为该序列化类生成 int ID，后续在序列化时使用 int ID 唯一标识该类型。
         */
        kryo.register(RpcResponse.class);
        kryo.register(RpcRequest.class);
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {
            //使用kryoThreadLocal 解决线程不安全
            Kryo kryo = kryoThreadLocal.get();
            // Object->byte:将对象序列化为byte数组
            kryo.writeObject(output, obj);
            kryoThreadLocal.remove();
            return output.toBytes();
        } catch (Exception e) {
            throw new SerializeException("Serialization failed");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            // byte->Object:从byte数组中反序列化出对对象
            Object o = kryo.readObject(input, clazz);
            kryoThreadLocal.remove();
            return clazz.cast(o);
        } catch (Exception e) {
            throw new SerializeException("Deserialization failed");
        }
    }

}
