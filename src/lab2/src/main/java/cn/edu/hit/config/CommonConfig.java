package cn.edu.hit.config;

public class CommonConfig {

    public static final int BUFFER_SIZE = 1028; // 缓冲区大小
    public static final int TIMEOUT = 1000; // 超时时间500ms
    public static final int DATA_SIZE = 1024; // 数据部分大小
    public static final double SENDER_PACKET_LOSS_RATE = 0.1; // 发送方丢包率
    public static final double RECEIVER_PACKET_LOSS_RATE = 0.1; // 接收方丢包率
    public static final int ACK_SIZE = 1; // ACK部分大小
}
