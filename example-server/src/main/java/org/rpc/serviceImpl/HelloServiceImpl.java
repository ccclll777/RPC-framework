package org.rpc.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.rpc.annotation.RpcService;
import org.rpc.Hello;
import org.rpc.HelloService;

/**
 *
 */
@Slf4j
@RpcService(group = "test1", version = "version1")
public class HelloServiceImpl implements HelloService {

    static {
        System.out.println("HelloServiceImpl被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl返回: {}.", result);
        return result;
    }
}
