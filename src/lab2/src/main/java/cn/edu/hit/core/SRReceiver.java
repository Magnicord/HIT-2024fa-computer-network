package cn.edu.hit.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import cn.edu.hit.config.CommonConfig;
import cn.edu.hit.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SRReceiver implements Receiver {

    // 接收端的 UDP 套接字
    private final DatagramSocket socket;
    // 服务器地址（发送端）
    private final InetAddress serverAddress;
    // 服务器端口
    private final int serverPort;
    // 窗口大小
    private final int windowSize;
    // 序列号的最大值
    private final int seqSize;
    // ACK 丢包率
    private final double packetLossRate;
    // 是否接收到数据包
    private final Packet[] packetsBuffered;
    // 是否接收到 EOF 包
    private boolean eof;
    // 期望的序列号
    private int base;

    /**
     * 构造方法，初始化 SRReceiver 对象
     *
     * @param socket 接收端的 UDP 套接字
     * @param serverAddress 服务器地址（发送端）
     * @param serverPort 服务器端口
     * @param windowSize 窗口大小
     * @param seqBits 序列号位数
     * @param packetLossRate ACK 丢包率
     */
    public SRReceiver(DatagramSocket socket, InetAddress serverAddress, int serverPort, int windowSize, int seqBits,
        double packetLossRate) {
        this.socket = socket;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.windowSize = windowSize;
        this.packetLossRate = packetLossRate;
        this.base = 0; // 初始期望的序列号为 0
        this.eof = false; // 初始时未接收到 EOF
        this.seqSize = (int)Math.pow(2, seqBits); // 计算序列号的最大值
        this.packetsBuffered = new Packet[seqSize];
    }

    @Override
    public void receiveData(String fileName) throws IOException {
        while (!eof) {
            byte[] receiveBuffer = new byte[CommonConfig.BUFFER_SIZE]; // 接收数据的缓冲区
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket); // 接收来自发送方的数据包
            Packet packet = Packet.fromBytes(receivePacket.getData()); // 解析数据包
            int seqNum = packet.getSeqNum();
            log.info("接收到数据包序列号: {}", seqNum);

            if (packet.isEof()) {
                eof = true;
                log.info("接收到 EOF 包，结束接收数据");
            }
            if (isInWindow(seqNum)) {
                if (Math.random() > packetLossRate) {
                    sendAck(seqNum);
                }
                packetsBuffered[seqNum] = packet;
                if (seqNum == base) {
                    while (packet != null) {
                        deliverPacket(packet, fileName);
                        packetsBuffered[base] = null;
                        base = (base + 1) % seqSize;
                        packet = packetsBuffered[base];
                    }
                }
            } else if (isInLastWindow(seqNum)) {
                if (Math.random() > packetLossRate) {
                    sendAck(seqNum);
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

    /**
     * 将数据包写入文件
     *
     * @param packet 数据包
     * @param fileName 文件名
     * @throws IOException 如果发生 I/O 错误
     */
    private void deliverPacket(Packet packet, String fileName) throws IOException {
        IOUtils.appendBytesToFile(fileName, packet.getData());
    }

    /**
     * 判断序列号是否在窗口内
     *
     * @param seqNum 序列号
     * @return 是否在窗口内
     */
    private boolean isInWindow(int seqNum) {
        int last = (base + windowSize - 1) % seqSize;
        if (base <= last) {
            return seqNum >= base && seqNum <= last;
        } else {
            return seqNum >= base || seqNum <= last;
        }
    }

    /**
     * 判断序列号是否在上一个窗口内
     *
     * @param seqNum 序列号
     * @return 是否在上一个窗口内
     */
    private boolean isInLastWindow(int seqNum) {
        int last = (this.base - 1 + seqSize) % seqSize;
        int base = (this.base - windowSize + seqSize) % seqSize;
        if (base <= last) {
            return seqNum >= base && seqNum <= last;
        } else {
            return seqNum >= base || seqNum <= last;
        }
    }
}
