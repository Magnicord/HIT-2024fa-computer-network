package cn.edu.hit.core;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public interface Receiver {

    /**
     * 创建 GBN 接收器
     *
     * @param socket 套接字，用于数据传输
     * @param serverAddress 服务器地址
     * @param serverPort 服务器端口
     * @param seqBits 序列号位数
     * @param packetLossRate 丢包率
     * @return GBN 接收器实例
     */
    static Receiver createGBNReceiver(DatagramSocket socket, InetAddress serverAddress, int serverPort, int seqBits,
        double packetLossRate) {
        return new GBNReceiver(socket, serverAddress, serverPort, seqBits, packetLossRate);
    }

    /**
     * 创建 SR 接收器
     *
     * @param socket 套接字，用于数据传输
     * @param serverAddress 服务器地址
     * @param serverPort 服务器端口
     * @param windowSize 窗口大小
     * @param seqBits 序列号位数
     * @param packetLossRate 丢包率
     * @return SR 接收器实例
     */
    static Receiver createSRReceiver(DatagramSocket socket, InetAddress serverAddress, int serverPort, int windowSize,
        int seqBits, double packetLossRate) {
        return new SRReceiver(socket, serverAddress, serverPort, windowSize, seqBits, packetLossRate);
    }

    /**
     * 接收数据并保存到文件
     *
     * @param fileName 文件名
     * @throws IOException 如果发生 I/O 错误
     */
    void receiveData(String fileName) throws IOException;
}
