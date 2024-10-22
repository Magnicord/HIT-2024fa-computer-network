package cn.edu.hit.config;

public class Config {

    public static final int SERVER_PORT = 12340; // 服务器端口
    public static final int BUFFER_SIZE = 1028; // 缓冲区大小
    public static final int TIMEOUT = 500; // 超时时间500ms
    public static final int WINDOW_SIZE = 5; // GBN的滑动窗口大小
    public static final int DATA_SIZE = 1024; // 数据部分大小
    public static final int ACK_SIZE = 1; // ACK部分大小
    public static final int SEQ_BITS = 3; // 序列号位数
    public static final double SENDER_PACKET_LOSS_RATE = 0.1; // 发送方丢包率
    public static final double RECEIVER_PACKET_LOSS_RATE = 0.1; // 接收方丢包率
}
