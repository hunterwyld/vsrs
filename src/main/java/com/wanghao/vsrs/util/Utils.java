package com.wanghao.vsrs.util;

/**
 * @author wanghao
 */
public class Utils {
    public static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (Byte.MIN_VALUE + (int)(Math.random()*(Byte.MAX_VALUE-Byte.MIN_VALUE+1)));
        }
        return bytes;
    }
}
