package cn.edu.hit.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import cn.edu.hit.config.CommonConfig;
import cn.edu.hit.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SRReceiver {

    private final DatagramSocket socket; // 接收端的UDP套接字
    private final InetAddress serverAddress; // 服务器地址（发送端）
    private final int serverPort; // 服务器端口
    private final int windowSize; // 窗口大小
    private final int seqSize; // 序列号的最大值
    private final double packetLossRate; // ACK丢包率
    private final Packet[] packetsBuffered; // 是否接收到数据包
    private final boolean[] packetsReceived; // 是否接收到数据包
    private boolean eof; // 是否接收到EOF包
    private int expectedSeqNum; // 期望的序列号

    public SRReceiver(DatagramSocket socket, InetAddress serverAddress, int serverPort, int windowSize, int seqBits,
        double packetLossRate) {
        this.socket = socket;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.windowSize = windowSize;
        this.packetLossRate = packetLossRate;
        this.expectedSeqNum = 0; // 初始期望的序列号为0
        this.eof = false; // 初始时未接收到EOF
        this.seqSize = (int)Math.pow(2, seqBits); // 计算序列号的最大值
        this.packetsBuffered = new Packet[seqSize];
        this.packetsReceived = new boolean[seqSize];
    }

    public void receiveData(String fileName) throws Exception {
        while (!eof) {
            byte[] receiveBuffer = new byte[CommonConfig.BUFFER_SIZE]; // 接收数据的缓冲区
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket); // 接收来自发送方的数据包
            Packet packet = Packet.fromBytes(receivePacket.getData()); // 解析数据包
            int seqNum = packet.getSeqNum();
            log.info("接收到数据包序列号: {}", seqNum);

            if (packet.isEof()) {
                eof = true;
                log.info("接收到EOF包，结束接收数据");
            }
            sendAck(seqNum);

            if (isInWindow(seqNum)) {
                packetsReceived[seqNum] = true;
                if (seqNum == expectedSeqNum) {
                    do {
                        deliverPacket(packet, fileName);
                        packetsBuffered[expectedSeqNum] = null;
                        expectedSeqNum = (expectedSeqNum + 1) % seqSize;
                        packet = packetsBuffered[expectedSeqNum];
                    } while (packet != null);
                }
            }
        }
    }

    private void sendAck(int seqNum) throws IOException {
        ACK ack = new ACK(seqNum); // 构造ACK对象
        byte[] ackBytes = ack.toBytes(); // 将ACK转换为字节数组
        DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, serverAddress, serverPort);
        socket.send(ackPacket); // 发送ACK
        log.info("发送ACK序列号: {}", seqNum);
    }

    private void deliverPacket(Packet packet, String fileName) throws IOException {
        IOUtils.appendBytesToFile(fileName, packet.getData());
    }

    private boolean isInWindow(int seqNum) {
        int last = (expectedSeqNum + windowSize - 1) % seqSize;
        if (expectedSeqNum <= last) {
            return seqNum >= expectedSeqNum && seqNum <= last;
        } else {
            return seqNum >= expectedSeqNum || seqNum <= last;
        }
    }
}
