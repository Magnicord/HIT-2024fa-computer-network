package cn.edu.hit.core;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

import cn.edu.hit.config.CommonConfig;
import cn.edu.hit.utils.IOUtils;
import cn.edu.hit.utils.Timer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GBNSender implements Sender {

    // 发送端的 UDP 套接字
    private final DatagramSocket socket;
    // 客户端地址
    private final InetAddress clientAddress;
    // 客户端端口号
    private final int clientPort;
    // 滑动窗口大小
    private final int windowSize;
    // 已发送但未确认的数据包
    private final ConcurrentHashMap<Integer, Packet> sentPackets;
    // 超时重传的计时器
    private final Timer timer;
    // 序列号的最大值
    private final int seqSize;
    // 丢包率
    private final double packetLossRate;
    // 滑动窗口的基序列号
    private int base;
    // 下一个要发送的序列号
    private int nextSeqNum;

    /**
     * 构造方法，初始化 GBNSender 对象
     *
     * @param socket 发送端的 UDP 套接字
     * @param clientAddress 客户端地址
     * @param clientPort 客户端端口号
     * @param windowSize 滑动窗口大小
     * @param seqBits 序列号位数
     * @param packetLossRate 丢包率
     * @param timeout 超时时间
     * @throws SocketException 如果创建套接字失败
     */
    public GBNSender(DatagramSocket socket, InetAddress clientAddress, int clientPort, int windowSize, int seqBits,
        double packetLossRate, long timeout) throws SocketException {
        this.socket = socket;
        this.packetLossRate = packetLossRate;
        this.socket.setSoTimeout(100); // 设置接收 ACK 的超时时间
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.windowSize = windowSize;
        this.base = 0; // 初始 base 值为 0
        this.nextSeqNum = 0; // 初始下一个要发送的序列号为 0
        this.seqSize = (int)Math.pow(2, seqBits); // 计算序列号的最大值
        this.sentPackets = new ConcurrentHashMap<>(seqSize); // 使用并发哈希表来存储已发送未确认的数据包
        this.timer = new Timer(timeout); // 初始化定时器
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

                // 保存发送的包
                sentPackets.put(nextSeqNum % seqSize, packet);

                // 如果要发送第一个未被确认的数据包，启动定时器
                if (base == nextSeqNum) {
                    startTimer();
                }

                nextSeqNum++; // 递增下一个要发送的序列号
            }

            // 处理 ACK
            log.info("等待接收 ACK...");
            receiveAck();
        }
        socket.setSoTimeout(0);
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
                byte[] ackBuffer = new byte[CommonConfig.ACK_SIZE]; // 1 字节的 ACK 缓冲区
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                socket.receive(ackPacket); // 接收 ACK 数据包
                ACK ack = ACK.fromBytes(ackPacket.getData()); // 使用 ACK 类解析 ACK 数据包
                int ackNum = ack.seqNum(); // 获取 ACK 的序列号

                log.info("接收到 ACK: {}", ackNum);
                // 计算 base 滑动窗口大小
                int slideSize = windowSlide(ackNum);
                if (slideSize > 0) { // 如果 base 滑动，即收到非重复的 ACK
                    received = true;
                    base += slideSize; // base 滑动
                    if (base == nextSeqNum) { // 如果窗口内没有未确认的数据包
                        log.info("当前窗口的数据包均已发送并确认，停止定时器");
                        timer.stop(); // 停止定时器
                    } else { // 否则重启定时器
                        startTimer();
                    }
                } else {
                    log.info("收到重复的 ACK: {}，继续接收 ACK", ackNum);
                }
            } catch (SocketTimeoutException ignored) { // 相当于将阻塞式的 receive 方法转换为非阻塞式，以便仅由 Timer 处理超时
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 处理超时
     *
     * @throws IOException 如果发生 I/O 错误
     */
    private void handleTimeout() throws IOException {
        // 重传窗口内的所有包
        log.info("超时重传窗口内的所有数据包");
        for (int i = base; i < nextSeqNum; i++) {
            sendPacket(sentPackets.get(i % seqSize));
        }
        startTimer(); // 重启定时器
    }

    /**
     * 启动定时器
     */
    private void startTimer() {
        timer.start(() -> {
            try {
                handleTimeout();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 计算窗口滑动大小
     *
     * @param ackNum ACK 序列号
     * @return 窗口滑动大小
     */
    private int windowSlide(int ackNum) {
        int base = this.base % seqSize;
        return (ackNum - base + 1 + seqSize) % seqSize;
    }
}
