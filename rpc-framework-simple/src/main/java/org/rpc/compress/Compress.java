package org.rpc.compress;


import org.rpc.extension.SPI;

/*
压缩和解压的接口
 */

@SPI
public interface Compress {

    byte[] compress(byte[] bytes);


    byte[] decompress(byte[] bytes);
}
