package cn.edu.hit.app;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Path;

import cn.edu.hit.config.CommonConfig;
import cn.edu.hit.config.GBNConfig;
import cn.edu.hit.config.SRConfig;
import cn.edu.hit.core.Sender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Server {

    private final DatagramSocket socket; // 服务器端的UDP套接字
    private Sender sender; // 发送方逻辑管理类

    public Server(int port) throws SocketException {
        socket = new DatagramSocket(port); // 绑定服务器端口
        log.info("服务器已启动，等待客户端连接...");
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(GBNConfig.SERVER_PORT); // 创建服务器
        server.waitForClient(); // 等待客户端连接
        String CWD = System.getProperty("user.dir");
        Path path = Path.of(CWD, "assets", "upload", "prince.txt");
        server.sendFile(path.toString()); // 发送文件数据
    }

    public void waitForClient() throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(requestPacket); // 接收来自客户端的初始请求
        // 客户端地址
        InetAddress clientAddress = requestPacket.getAddress(); // 获取客户端地址
        // 客户端端口
        int clientPort = requestPacket.getPort(); // 获取客户端端口
        log.info("服务器已连接到客户端 {}:{}", clientAddress, clientPort);

        // 初始化Sender类，用于数据传输
        // sender = Sender.createGBNSender(socket, clientAddress, clientPort, GBNConfig.WINDOW_SIZE, GBNConfig.SEQ_BITS,
        // CommonConfig.SENDER_PACKET_LOSS_RATE, CommonConfig.TIMEOUT);
        sender = Sender.createSRSender(socket, clientAddress, clientPort, SRConfig.SENDER_WINDOW_SIZE,
            SRConfig.SEQ_BITS, CommonConfig.SENDER_PACKET_LOSS_RATE, CommonConfig.TIMEOUT);
    }

    public void sendFile(String fileName) throws IOException {
        log.info("开始发送文件...");
        sender.sendFile(fileName); // 调用Sender类发送数据
        log.info("文件发送完毕！");
    }
}
