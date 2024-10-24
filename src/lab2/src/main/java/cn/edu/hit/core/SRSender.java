package cn.edu.hit.core;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

import cn.edu.hit.config.CommonConfig;
import cn.edu.hit.utils.IOUtils;
import cn.edu.hit.utils.Timer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SRSender implements Sender {

    private final DatagramSocket socket; // 发送端的UDP套接字
    private final InetAddress clientAddress; // 客户端地址
    private final int clientPort; // 客户端端口号
    private final int windowSize; // 发送窗口大小
    private final int seqSize; // 序列号范围
    private final double packetLossRate; // 丢包率
    private final ConcurrentHashMap<Integer, Packet> sentPackets; // 已发送但未确认的数据包
    private final Timer[] timers; // 计时器数组
    private final boolean[] ackReceived; // 存储ACK接收状态
    private int nextSeqNum; // 下一个待发送的序列号
    private int base; // 窗口的基序列号

    public SRSender(DatagramSocket socket, InetAddress clientAddress, int clientPort, int windowSize, int seqBits,
        double packetLossRate, long timeout) throws SocketException {
        this.socket = socket;
        this.socket.setSoTimeout(100); // 设置接收ACK的超时时间
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.windowSize = windowSize;
        this.seqSize = (int)Math.pow(2, seqBits);
        this.packetLossRate = packetLossRate;
        this.sentPackets = new ConcurrentHashMap<>(seqSize);
        this.ackReceived = new boolean[seqSize];
        this.nextSeqNum = 0;
        this.base = 0;
        this.timers = new Timer[seqSize];
        for (int i = 0; i < seqSize; i++) {
            timers[i] = new Timer(timeout);
        }
    }

    @Override
    public void sendFile(String fileName) throws IOException {
        byte[] data = IOUtils.readBytesFromFile(fileName); // 读取文件数据
        sendData(data); // 发送数据
    }

    @Override
    public void sendData(byte[] data) throws IOException {
        int totalPackets = (int)Math.ceil((double)data.length / CommonConfig.DATA_SIZE); // 计算总的数据包数
        byte[] sendBuffer = new byte[CommonConfig.DATA_SIZE]; // 创建1024字节的缓冲区
        // 当滑动窗口未覆盖所有数据包时
        while (base < totalPackets) {
            log.info("当前发送进度: {}/{}", base, totalPackets);
            // 发送方不会发送超过窗口大小的数据包，并且不会发送超过总数据包数的数据包
            while (nextSeqNum < base + windowSize && nextSeqNum < totalPackets) {
                // 计算当前包的实际大小 (<= 1024)
                int dataLength = Math.min(CommonConfig.DATA_SIZE, data.length - nextSeqNum * CommonConfig.DATA_SIZE);
                System.arraycopy(data, nextSeqNum * CommonConfig.DATA_SIZE, sendBuffer, 0, dataLength); // 复制数据到包中

                // 判断是否为最后一个数据包（EOF）
                boolean eof = (nextSeqNum == totalPackets - 1);
                Packet packet = new Packet(nextSeqNum % seqSize, sendBuffer, dataLength, eof);

                // 模拟丢包
                if (Math.random() > packetLossRate) {
                    sendPacket(packet); // 发送数据包
                }

                // 保存发送的包
                sentPackets.put(nextSeqNum % seqSize, packet);

                // if (base == nextSeqNum) {
                // for (int i = base; i < base + windowSize && i < totalPackets; i++) {
                // ackReceived[i % seqSize] = false;
                // }
                // }

                // 启动计时器
                startTimer(nextSeqNum % seqSize);
                nextSeqNum++;
            }
            receiveAck();
        }
    }

    private void sendPacket(Packet packet) throws IOException {
        byte[] packetBytes = packet.toBytes(); // 将Packet对象转换为字节数组
        DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, clientAddress, clientPort);
        socket.send(datagramPacket); // 发送UDP数据包
        log.info("发送数据包的序列号: {}", packet.getSeqNum());
    }

    // 处理ACK的接收
    private void receiveAck() {
        boolean received = false; // 是否收到 ACK
        while (!received) {
            try {
                byte[] ackBytes = new byte[CommonConfig.ACK_SIZE]; // 创建ACK字节数组
                DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length); // 创建ACK数据包
                socket.receive(ackPacket); // 接收ACK数据包
                ACK ack = ACK.fromBytes(ackBytes); // 将字节数组转换为Packet对象
                int ackNum = ack.seqNum(); // 获取ACK的序列号
                log.info("接收到ACK: {}", ackNum);
                received = handleAck(ackNum); // 处理ACK
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                log.error("接收ACK失败: {}", e.getMessage());
            }
        }
    }

    private boolean handleAck(int ackNum) {
        // if (ackReceived[ackNum]) {
        // return false;
        // }

        if (isInWindow(ackNum)) {
            ackReceived[ackNum] = true;
            if (ackNum == base % seqSize) {
                // int slideSize = 1;
                // while (base + slideSize < nextSeqNum && ackReceived[(base + slideSize) % seqSize]) {
                // slideSize++;
                // }
                // base += slideSize;

                // do {
                // // timers[base % seqSize].stop();
                // base++;
                // } while (base < nextSeqNum && ackReceived[base % seqSize]);

                while (base < nextSeqNum && ackReceived[base % seqSize]) {
                    timers[base % seqSize].stop();
                    ackReceived[base % seqSize] = false; // 重置ACK接收状态
                    base++;
                }
                return true;
            }
        }
        return false;
    }

    private void handleTimeout(int seqNum) throws IOException {
        if (ackReceived[seqNum]) {
            return;
        }
        log.info("超时重传窗口内序列号为{}的数据包", seqNum);
        sendPacket(sentPackets.get(seqNum));
        startTimer(seqNum); // 重启定时器
    }

    private void startTimer(int seqNum) {
        timers[seqNum].start(() -> {
            try {
                handleTimeout(seqNum);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean isInWindow(int seqNum) {
        int last = (base + windowSize - 1) % seqSize;
        int base = this.base % seqSize;
        if (base <= last) {
            return seqNum >= base && seqNum <= last;
        } else {
            return seqNum >= base || seqNum <= last;
        }
    }
}
