package cn.edu.hit.app;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;

import cn.edu.hit.config.Config;
import cn.edu.hit.core.Receiver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Client {

    private final DatagramSocket socket; // 客户端的UDP套接字
    private final Receiver receiver; // 接收方逻辑管理类
    private final InetAddress serverAddress; // 服务器地址

    public Client(String serverIp, int serverPort) throws Exception {
        socket = new DatagramSocket(); // 创建UDP套接字
        serverAddress = InetAddress.getByName(serverIp); // 获取服务器地址
        log.info("客户端已连接到服务器 {}:{}", serverIp, serverPort);

        // 初始化Receiver类，用于接收数据包
        receiver = new Receiver(socket, serverAddress, serverPort, Config.SEQ_BITS, Config.RECEIVER_PACKET_LOSS_RATE);
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client("127.0.0.1", Config.SERVER_PORT); // 创建客户端，连接服务器
        client.sendConnectionRequest(); // 发送连接请求
        String CWD = System.getProperty("user.dir");
        Path path = Path.of(CWD, "assets", "download", "campus.jpg");
        client.receiveFile(path.toString()); // 接收文件数据
    }

    /**
     * 发送连接请求
     */
    public void sendConnectionRequest() throws Exception {
        String requestMessage = "REQUEST_CONNECTION";
        byte[] requestBuffer = requestMessage.getBytes();
        DatagramPacket requestPacket =
            new DatagramPacket(requestBuffer, requestBuffer.length, serverAddress, Config.SERVER_PORT);
        socket.send(requestPacket); // 发送连接请求
        log.info("客户端已发送连接请求...");
    }

    /**
     * 接收文件数据
     */
    public void receiveFile(String fileName) throws Exception {
        log.info("客户端开始接收文件...");
        receiver.receiveData(fileName);
        log.info("客户端文件接收完毕！");
    }
}