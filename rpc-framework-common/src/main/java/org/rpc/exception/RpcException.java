package org.rpc.exception;

import org.rpc.enums.RpcErrorMessageEnum;

public class RpcException extends RuntimeException {
    /*
    异常类型 集成了RuntimeException
     */
    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum, String detail) {
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum) {
        super(rpcErrorMessageEnum.getMessage());
    }
}
