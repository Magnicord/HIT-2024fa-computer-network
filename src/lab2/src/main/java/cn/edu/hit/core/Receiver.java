package cn.edu.hit.core;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import cn.edu.hit.config.CommonConfig;
import cn.edu.hit.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Receiver {

    private final DatagramSocket socket; // 接收端的UDP套接字
    private final InetAddress serverAddress; // 服务器地址（发送端）
    private final int serverPort; // 服务器端口
    private final int seqSize; // 序列号的最大值
    private final double packetLossRate; // ACK丢包率
    private int expectedSeqNum; // 期望的序列号
    private boolean eof; // 是否接收到EOF包

    public Receiver(DatagramSocket socket, InetAddress serverAddress, int serverPort, int seqBits,
        double packetLossRate) {
        this.socket = socket;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.packetLossRate = packetLossRate;
        this.expectedSeqNum = 0; // 初始期望的序列号为0
        this.eof = false; // 初始时未接收到EOF
        this.seqSize = (int)Math.pow(2, seqBits); // 计算序列号的最大值
    }

    public void receiveData(String fileName) throws Exception {
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

                // 发送ACK，确认该包
                if (Math.random() > packetLossRate) {
                    sendAck(expectedSeqNum);
                }

                // 更新期望的下一个序列号
                expectedSeqNum = (expectedSeqNum + 1) % seqSize;

                // 检查是否是EOF包
                if (packet.isEof()) {
                    eof = true;
                    log.info("接收到EOF包，结束接收数据");
                }
            } else {
                // 乱序包，发送之前的ACK
                log.info("接收到乱序包，期望的序列号: {}，收到的序列号: {}", expectedSeqNum, packet.getSeqNum());
                sendAck((expectedSeqNum - 1 + seqSize) % seqSize); // 重发之前的ACK
            }
        }
    }

    private void sendAck(int seqNum) throws Exception {
        ACK ack = new ACK(seqNum); // 构造ACK对象
        byte[] ackBytes = ack.toBytes(); // 将ACK转换为字节数组
        DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, serverAddress, serverPort);
        socket.send(ackPacket); // 发送ACK
        log.info("发送ACK序列号: {}", seqNum);
    }
}
