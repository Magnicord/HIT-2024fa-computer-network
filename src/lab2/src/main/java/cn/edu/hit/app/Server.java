package cn.edu.hit.app;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;

import cn.edu.hit.config.Config;
import cn.edu.hit.core.Sender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Server {

    private final DatagramSocket socket; // 服务器端的UDP套接字
    private Sender sender; // 发送方逻辑管理类
    private InetAddress clientAddress; // 客户端地址
    private int clientPort; // 客户端端口

    public Server(int port) throws Exception {
        socket = new DatagramSocket(port); // 绑定服务器端口
        log.info("服务器已启动，等待客户端连接...");
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(Config.SERVER_PORT); // 创建服务器
        server.waitForClient(); // 等待客户端连接
        String CWD = System.getProperty("user.dir");
        Path path = Path.of(CWD, "assets", "upload", "campus.jpg");
        server.sendFile(path.toString()); // 发送文件数据
    }

    /**
     * 等待客户端连接（实际只是接收初始的消息，用来获取客户端的地址和端口）
     */
    public void waitForClient() throws Exception {
        byte[] buffer = new byte[1024];
        DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(requestPacket); // 接收来自客户端的初始请求
        clientAddress = requestPacket.getAddress(); // 获取客户端地址
        clientPort = requestPacket.getPort(); // 获取客户端端口
        log.info("服务器已连接到客户端 {}:{}", clientAddress, clientPort);

        // 初始化Sender类，用于数据传输
        sender = new Sender(socket, clientAddress, clientPort, Config.WINDOW_SIZE, Config.SEQ_BITS,
            Config.SENDER_PACKET_LOSS_RATE, Config.TIMEOUT);
    }

    /**
     * 发送文件数据
     */
    public void sendFile(String fileName) throws Exception {
        log.info("开始发送文件...");
        sender.sendFile(fileName); // 调用Sender类发送数据
        log.info("文件发送完毕！");
    }
}
