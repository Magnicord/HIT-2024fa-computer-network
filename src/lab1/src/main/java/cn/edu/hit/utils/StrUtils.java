package cn.edu.hit.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 字符串工具类，提供字符串相关的实用方法。
 */
public class StrUtils {

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            // 将每个字节转换为十六进制表示
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                // 如果十六进制字符串长度为 1，则在前面补 0
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 计算字符串的 SHA-256 哈希值。
     *
     * @param str 输入字符串
     * @return 哈希值的十六进制字符串
     */
    public static String hashString(String str) {
        try {
            // 获取 SHA-256 消息摘要实例
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 计算哈希值
            byte[] hash = digest.digest(str.getBytes());
            // 将哈希值转换为十六进制字符串
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // 如果没有找到 SHA-256 算法，抛出运行时异常
            throw new RuntimeException(e);
        }
    }
}
