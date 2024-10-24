package cn.edu.hit.core;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public interface Sender {

    /**
     * 创建 GBN 发送器
     *
     * @param socket 套接字，用于数据传输
     * @param clientAddress 客户端地址
     * @param clientPort 客户端端口
     * @param windowSize 窗口大小
     * @param seqBits 序列号位数
     * @param packetLossRate 丢包率
     * @param timeout 超时时间
     * @return GBN 发送器实例
     * @throws SocketException 如果创建套接字失败
     */
    static Sender createGBNSender(DatagramSocket socket, InetAddress clientAddress, int clientPort, int windowSize,
        int seqBits, double packetLossRate, long timeout) throws SocketException {
        return new GBNSender(socket, clientAddress, clientPort, windowSize, seqBits, packetLossRate, timeout);
    }

    /**
     * 创建 SR 发送器
     *
     * @param socket 套接字，用于数据传输
     * @param clientAddress 客户端地址
     * @param clientPort 客户端端口
     * @param windowSize 窗口大小
     * @param seqBits 序列号位数
     * @param packetLossRate 丢包率
     * @param timeout 超时时间
     * @return SR 发送器实例
     * @throws SocketException 如果创建套接字失败
     */
    static Sender createSRSender(DatagramSocket socket, InetAddress clientAddress, int clientPort, int windowSize,
        int seqBits, double packetLossRate, long timeout) throws SocketException {
        return new SRSender(socket, clientAddress, clientPort, windowSize, seqBits, packetLossRate, timeout);
    }

    /**
     * 发送文件
     *
     * @param fileName 文件名
     * @throws IOException 如果发生 I/O 错误
     */
    void sendFile(String fileName) throws IOException;

    /**
     * 发送数据
     *
     * @param data 数据字节数组
     * @throws IOException 如果发生 I/O 错误
     */
    void sendData(byte[] data) throws IOException;
}
