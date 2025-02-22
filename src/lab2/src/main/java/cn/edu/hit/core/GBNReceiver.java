package cn.edu.hit.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import cn.edu.hit.config.CommonConfig;
import cn.edu.hit.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GBNReceiver implements Receiver {

    // 接收端的 UDP 套接字
    private final DatagramSocket socket;
    // 服务器地址（发送端）
    private final InetAddress serverAddress;
    // 服务器端口
    private final int serverPort;
    // 序列号的最大值
    private final int seqSize;
    // ACK 丢包率
    private final double packetLossRate;
    // 期望的序列号
    private int expectedSeqNum;
    // 是否接收到 EOF 包
    private boolean eof;

    /**
     * 构造方法，初始化 GBNReceiver 对象
     *
     * @param socket 接收端的 UDP 套接字
     * @param serverAddress 服务器地址（发送端）
     * @param serverPort 服务器端口
     * @param seqBits 序列号位数
     * @param packetLossRate ACK 丢包率
     */
    public GBNReceiver(DatagramSocket socket, InetAddress serverAddress, int serverPort, int seqBits,
        double packetLossRate) {
        this.socket = socket;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.packetLossRate = packetLossRate;
        this.expectedSeqNum = 0; // 初始期望的序列号为 0
        this.eof = false; // 初始时未接收到 EOF
        this.seqSize = (int)Math.pow(2, seqBits); // 计算序列号的最大值
    }

    @Override
    public void receiveData(String fileName) throws IOException {
        while (!eof) {
            byte[] receiveBuffer = new byte[CommonConfig.BUFFER_SIZE]; // 接收数据的缓冲区
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket); // 接收来自发送方的数据包
            Packet packet = Packet.fromBytes(receivePacket.getData()); // 解析数据包

            log.info("接收到数据包序列号: {}", packet.getSeqNum());

            // 检查是否是期望的序列号
            if (packet.getSeqNum() == expectedSeqNum) {
                // 如果是期望的包，将数据写入文件
                IOUtils.appendBytesToFile(fileName, packet.getData());

                // 发送 ACK，确认该包
                if (Math.random() > packetLossRate) {
                    sendAck(expectedSeqNum);
                }

                // 更新期望的下一个序列号
                expectedSeqNum = (expectedSeqNum + 1) % seqSize;

                // 检查是否是 EOF 包
                if (packet.isEof()) {
                    eof = true;
                    log.info("接收到 EOF 包，结束接收数据");
                }
            } else {
                // 乱序包，发送之前的 ACK
                log.info("接收到乱序包，期望的序列号: {}，收到的序列号: {}", expectedSeqNum, packet.getSeqNum());
                if (Math.random() > packetLossRate) {
                    sendAck((expectedSeqNum - 1 + seqSize) % seqSize); // 重发之前的 ACK
                }
            }
        }
    }

    /**
     * 发送 ACK
     *
     * @param seqNum ACK 序列号
     * @throws IOException 如果发生 I/O 错误
     */
    private void sendAck(int seqNum) throws IOException {
        ACK ack = new ACK(seqNum); // 构造 ACK 对象
        byte[] ackBytes = ack.toBytes(); // 将 ACK 转换为字节数组
        DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, serverAddress, serverPort);
        socket.send(ackPacket); // 发送 ACK
        log.info("发送 ACK 序列号: {}", seqNum);
    }
}
