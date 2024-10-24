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

    // 发送端的 UDP 套接字
    private final DatagramSocket socket;
    // 客户端地址
    private final InetAddress clientAddress;
    // 客户端端口号
    private final int clientPort;
    // 发送窗口大小
    private final int windowSize;
    // 序列号范围
    private final int seqSize;
    // 丢包率
    private final double packetLossRate;
    // 已发送但未确认的数据包
    private final ConcurrentHashMap<Integer, Packet> sentPackets;
    // 计时器数组
    private final Timer[] timers;
    // 存储 ACK 接收状态
    private final boolean[] ackReceived;
    // 下一个待发送的序列号
    private int nextSeqNum;
    // 窗口的基序列号
    private int base;

    /**
     * 构造方法，初始化 SRSender 对象
     *
     * @param socket 发送端的 UDP 套接字
     * @param clientAddress 客户端地址
     * @param clientPort 客户端端口号
     * @param windowSize 发送窗口大小
     * @param seqBits 序列号位数
     * @param packetLossRate 丢包率
     * @param timeout 超时时间
     * @throws SocketException 如果创建套接字失败
     */
    public SRSender(DatagramSocket socket, InetAddress clientAddress, int clientPort, int windowSize, int seqBits,
        double packetLossRate, long timeout) throws SocketException {
        this.socket = socket;
        this.socket.setSoTimeout(100); // 设置接收 ACK 的超时时间
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.windowSize = windowSize;
        this.seqSize = (int)Math.pow(2, seqBits); // 计算序列号范围
        this.packetLossRate = packetLossRate;
        this.sentPackets = new ConcurrentHashMap<>(seqSize); // 初始化已发送但未确认的数据包
        this.ackReceived = new boolean[seqSize]; // 初始化 ACK 接收状态数组
        this.nextSeqNum = 0; // 初始化下一个待发送的序列号
        this.base = 0; // 初始化窗口的基序列号
        this.timers = new Timer[seqSize]; // 初始化计时器数组
        for (int i = 0; i < seqSize; i++) {
            timers[i] = new Timer(timeout); // 初始化每个计时器
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
        byte[] sendBuffer = new byte[CommonConfig.DATA_SIZE]; // 创建 1024 字节的缓冲区
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
                sentPackets.put(nextSeqNum % seqSize, packet); // 保存发送的包
                startTimer(nextSeqNum % seqSize); // 启动定时器
                nextSeqNum++; // 递增下一个待发送的序列号
            }
            receiveAck(); // 处理 ACK
        }
    }

    /**
     * 发送数据包
     *
     * @param packet 数据包
     * @throws IOException 如果发生 I/O 错误
     */
    private void sendPacket(Packet packet) throws IOException {
        byte[] packetBytes = packet.toBytes(); // 将 Packet 对象转换为字节数组
        DatagramPacket datagramPacket = new DatagramPacket(packetBytes, packetBytes.length, clientAddress, clientPort);
        socket.send(datagramPacket); // 发送 UDP 数据包
        log.info("发送数据包的序列号: {}", packet.getSeqNum());
    }

    /**
     * 接收 ACK
     */
    private void receiveAck() {
        boolean received = false; // 是否收到 ACK
        while (!received) {
            try {
                byte[] ackBytes = new byte[CommonConfig.ACK_SIZE]; // 创建 ACK 字节数组
                DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length); // 创建 ACK 数据包
                socket.receive(ackPacket); // 接收 ACK 数据包
                ACK ack = ACK.fromBytes(ackBytes); // 将字节数组转换为 Packet 对象
                int ackNum = ack.seqNum(); // 获取 ACK 的序列号
                log.info("接收到 ACK: {}", ackNum);
                received = handleAck(ackNum); // 处理 ACK
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                log.error("接收 ACK 失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 处理 ACK
     *
     * @param ackNum ACK 序列号
     * @return 是否成功处理 ACK
     */
    private boolean handleAck(int ackNum) {
        if (isInWindow(ackNum)) {
            ackReceived[ackNum] = true; // 标记 ACK 已接收
            if (ackNum == base % seqSize) {
                while (base < nextSeqNum && ackReceived[base % seqSize]) {
                    timers[base % seqSize].stop(); // 停止计时器
                    ackReceived[base % seqSize] = false; // 重置 ACK 接收状态
                    base++; // 滑动窗口
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 处理超时
     *
     * @param seqNum 序列号
     * @throws IOException 如果发生 I/O 错误
     */
    private void handleTimeout(int seqNum) throws IOException {
        if (ackReceived[seqNum]) {
            return;
        }
        log.info("超时重传窗口内序列号为 {} 的数据包", seqNum);
        sendPacket(sentPackets.get(seqNum)); // 重传数据包
        startTimer(seqNum); // 重启定时器
    }

    /**
     * 启动定时器
     *
     * @param seqNum 序列号
     */
    private void startTimer(int seqNum) {
        timers[seqNum].start(() -> {
            try {
                handleTimeout(seqNum);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 判断序列号是否在窗口内
     *
     * @param seqNum 序列号
     * @return 是否在窗口内
     */
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
