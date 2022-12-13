package org.rpc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wangtao .
 * @createTime on 2020/10/2
 */
@AllArgsConstructor
@Getter
public enum CompressTypeEnum {
    /*
    压缩类型的枚举类
     */
    GZIP((byte) 0x01, "gzip");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
