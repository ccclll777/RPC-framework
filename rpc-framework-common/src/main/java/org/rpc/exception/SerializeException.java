package org.rpc.exception;

/*
序列化时产生的异常
 */
public class SerializeException extends RuntimeException {
    public SerializeException(String message) {
        super(message);
    }
}
