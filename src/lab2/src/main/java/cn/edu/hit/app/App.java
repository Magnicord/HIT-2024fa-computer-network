package cn.edu.hit.app;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import cn.edu.hit.config.AppConfig;
import cn.edu.hit.config.CommonConfig;
import cn.edu.hit.config.GBNConfig;
import cn.edu.hit.config.SRConfig;
import cn.edu.hit.core.Receiver;
import cn.edu.hit.core.Sender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

    // 服务器地址
    private final InetAddress serverAddress;
    // 目标端口
    private final int dstPort;
    // 源端口
    private final int srcPort;
    // 输入扫描器
    private final Scanner in = new Scanner(System.in);
    // 接收方逻辑管理类
    private Receiver receiver;
    // 接收方的 UDP 套接字
    private DatagramSocket clientSocket;
    // 发送方的 UDP 套接字
    private DatagramSocket serverSocket;
    // 发送方逻辑管理类
    private Sender sender;
    // 客户端端口
    private int clientPort;
    // 客户端地址
    private InetAddress clientAddress;

    /**
     * 构造方法，初始化 App 对象
     *
     * @param dstIp 目标 IP 地址
     * @param dstPort 目标端口
     * @param srcPort 源端口
     * @throws UnknownHostException 如果无法解析 IP 地址
     */
    public App(String dstIp, int dstPort, int srcPort) throws UnknownHostException {
        this.serverAddress = InetAddress.getByName(dstIp); // 获取服务器地址
        this.dstPort = dstPort;
        this.srcPort = srcPort;
    }

    /**
     * 发送字符串数据包
     *
     * @param str 要发送的字符串
     * @param socket 套接字
     * @param address 目标地址
     * @param port 目标端口
     */
    private static void sendString(String str, DatagramSocket socket, InetAddress address, int port) {
        byte[] sendBuffer = str.getBytes(); // 将字符串转换为字节数组
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
        try {
            socket.send(sendPacket); // 发送数据包
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取文件路径
     *
     * @param mode 模式（上传或下载）
     * @param fileName 文件名
     * @return 文件路径
     */
    private static String getFilePath(String mode, String fileName) {
        String CWD = System.getProperty("user.dir"); // 获取当前工作目录
        Path path = Path.of(CWD, "assets", mode, fileName);
        return path.toString();
    }

    /**
     * 作为服务器接收字符串数据包
     *
     * @return 接收到的字符串
     */
    private String receiveStringAsServer() {
        byte[] receiveBuffer = new byte[1024]; // 接收缓冲区
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        try {
            serverSocket.receive(receivePacket); // 接收数据包
            clientAddress = receivePacket.getAddress(); // 获取客户端地址
            clientPort = receivePacket.getPort(); // 获取客户端端口
            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 启动应用
     *
     * @throws IOException 如果发生 I/O 错误
     */
    public void start() throws IOException {
        boolean isExit = false;
        while (!isExit) {
            System.out.println();
            System.out.println("---------------------------------");
            System.out.println("1. 服务器模式 上传文件");
            System.out.println("2. 客户端模式 下载文件");
            System.out.println("3. 退出应用");
            System.out.println("---------------------------------");
            System.out.print("请输入数字，选择模式: ");
            int choice = in.nextInt();
            in.nextLine(); // 消耗换行符
            switch (choice) {
                case 1 -> uploadMode();
                case 2 -> downloadMode();
                case 3 -> isExit = true;
                default -> System.out.println("无效的选择，请重新输入选项！");
            }
        }
        in.close();
    }

    /**
     * 客户端下载模式
     *
     * @throws IOException 如果发生 I/O 错误
     */
    private void downloadMode() throws IOException {
        boolean isExit = false;
        while (!isExit) {
            clientSocket = new DatagramSocket(); // 创建接收方 UDP 套接字
            System.out.println();
            System.out.println("进入客户端下载模式，以下是可输入的命令");
            System.out.println("---------------------------------");
            System.out.println(AppConfig.TIME_PROMPT + ": " + AppConfig.TIME_PROMPT_HELP);
            System.out.println(AppConfig.GBN_PROMPT + ": " + AppConfig.GBN_PROMPT_HELP);
            System.out.println(AppConfig.SR_PROMPT + ": " + AppConfig.SR_PROMPT_HELP);
            System.out.println(AppConfig.QUIT_PROMPT + ": " + AppConfig.QUIT_PROMPT_HELP);
            System.out.println("---------------------------------");
            System.out.print("请输入命令: ");
            String prompt = in.nextLine();
            switch (prompt) {
                case AppConfig.TIME_PROMPT -> {
                    sendString(prompt, clientSocket, serverAddress, dstPort);
                    String msg = receiveStringAsClient();
                    System.out.println(msg);
                }
                case AppConfig.QUIT_PROMPT -> {
                    sendString(prompt, clientSocket, serverAddress, dstPort);
                    String msg = receiveStringAsClient();
                    System.out.println(msg);
                    isExit = true;
                }
                default -> {
                    if (!prompt.startsWith(AppConfig.GBN_PROMPT_PREFIX)
                        && !prompt.startsWith(AppConfig.SR_PROMPT_PREFIX)) {
                        System.out.println("无效的命令，请重新输入！");
                    } else {
                        String[] args = prompt.split(" ");
                        if (args.length != 2) {
                            System.out.println("无效的命令，请重新输入！");
                            break;
                        }
                        sendString(prompt, clientSocket, serverAddress, dstPort);
                        if (prompt.startsWith(AppConfig.GBN_PROMPT_PREFIX)) {
                            receiver = Receiver.createGBNReceiver(clientSocket, serverAddress, dstPort,
                                GBNConfig.SEQ_BITS, CommonConfig.RECEIVER_PACKET_LOSS_RATE);
                        } else {
                            receiver = Receiver.createSRReceiver(clientSocket, serverAddress, dstPort,
                                SRConfig.RECEIVER_WINDOW_SIZE, SRConfig.SEQ_BITS,
                                CommonConfig.RECEIVER_PACKET_LOSS_RATE);
                        }
                        String fileName = args[1];
                        receiveFile(fileName);
                    }
                }
            }
        }
    }

    /**
     * 服务器上传模式
     *
     * @throws IOException 如果发生 I/O 错误
     */
    private void uploadMode() throws IOException {
        serverSocket = new DatagramSocket(srcPort); // 创建发送方 UDP 套接字
        System.out.println();
        System.out.println("进入服务器上传模式，等待客户端发送命令...");
        boolean isExit = false;
        while (!isExit) {
            String prompt = receiveStringAsServer();
            switch (prompt) {
                case AppConfig.TIME_PROMPT -> {
                    System.out.println("客户端请求获取时间...");
                    LocalDateTime now = LocalDateTime.now();
                    String time = now.format(DateTimeFormatter.ofPattern(AppConfig.TIME_FORMAT));
                    System.out.println("当前时间为: " + time);
                    sendString(time, serverSocket, serverAddress, clientPort);
                }
                case AppConfig.QUIT_PROMPT -> {
                    System.out.println("客户端请求退出...");
                    sendString(AppConfig.QUIT_REPLY_MSG, serverSocket, serverAddress, clientPort);
                }
                default -> {
                    if (!prompt.startsWith(AppConfig.GBN_PROMPT_PREFIX)
                        && !prompt.startsWith(AppConfig.SR_PROMPT_PREFIX)) {
                        System.out.println("客户端发送无效的命令: " + prompt);
                        sendString(prompt, serverSocket, clientAddress, clientPort);
                    } else {
                        String[] args = prompt.split(" ");
                        if (args.length != 2) {
                            System.out.println("客户端发送无效的命令: " + prompt);
                            sendString(prompt, serverSocket, clientAddress, clientPort);
                            break;
                        }
                        if (prompt.startsWith(AppConfig.GBN_PROMPT_PREFIX)) {
                            System.out.println("客户端请求测试 GBN 协议...");
                            sender =
                                Sender.createGBNSender(serverSocket, clientAddress, clientPort, GBNConfig.WINDOW_SIZE,
                                    GBNConfig.SEQ_BITS, CommonConfig.SENDER_PACKET_LOSS_RATE, CommonConfig.TIMEOUT);
                        } else {
                            System.out.println("客户端请求测试 SR 协议...");
                            sender = Sender.createSRSender(serverSocket, clientAddress, clientPort,
                                SRConfig.SENDER_WINDOW_SIZE, SRConfig.SEQ_BITS, CommonConfig.SENDER_PACKET_LOSS_RATE,
                                CommonConfig.TIMEOUT);
                        }
                        String fileName = args[1];
                        System.out.println("客户端请求下载文件: " + fileName);
                        sendFile(fileName);

                        System.out.println("是否退出服务器模式(y/n)");
                        System.out.print("请输入命令(y/n): ");
                        String exit = in.nextLine();
                        if (exit.equalsIgnoreCase("y")) {
                            isExit = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * 发送文件
     *
     * @param fileName 文件名
     * @throws IOException 如果发生 I/O 错误
     */
    private void sendFile(String fileName) throws IOException {
        log.info("开始发送文件...");
        fileName = getFilePath("upload", fileName); // 获取文件路径
        sender.sendFile(fileName); // 发送文件
        log.info("文件发送完毕！");
    }

    /**
     * 接收文件
     *
     * @param fileName 文件名
     * @throws IOException 如果发生 I/O 错误
     */
    private void receiveFile(String fileName) throws IOException {
        log.info("客户端开始接收文件...");
        fileName = getFilePath("download", fileName); // 获取文件路径
        receiver.receiveData(fileName); // 接收文件
        log.info("客户端文件接收完毕！");
    }

    /**
     * 作为客户端接收字符串数据包
     *
     * @return 接收到的字符串
     */
    private String receiveStringAsClient() {
        byte[] receiveBuffer = new byte[1024]; // 接收缓冲区
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        try {
            clientSocket.receive(receivePacket); // 接收数据包
            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
