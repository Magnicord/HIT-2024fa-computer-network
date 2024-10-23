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
import cn.edu.hit.core.Receiver;
import cn.edu.hit.core.Sender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

    private final InetAddress serverAddress; // 服务器地址
    private final int dstPort;
    private final int srcPort;
    private final Scanner in = new Scanner(System.in);
    private Receiver receiver; // 接收方逻辑管理类
    private DatagramSocket clientSocket; // 接收方的UDP套接字
    private DatagramSocket serverSocket; // 发送方的UDP套接字
    private Sender sender;
    private int clientPort; // 客户端端口
    private InetAddress clientAddress; // 客户端地址

    public App(String dstIp, int dstPort, int srcPort) throws UnknownHostException {
        this.serverAddress = InetAddress.getByName(dstIp); // 获取服务器地址
        this.dstPort = dstPort;
        this.srcPort = srcPort;
    }

    private static void sendString(String str, DatagramSocket socket, InetAddress address, int port) {
        byte[] sendBuffer = str.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFilePath(String mode, String fileName) {
        String CWD = System.getProperty("user.dir");
        Path path = Path.of(CWD, "assets", mode, fileName);
        return path.toString();
    }

    private String receiveStringAsServer() {
        byte[] receiveBuffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        try {
            serverSocket.receive(receivePacket);
            clientAddress = receivePacket.getAddress(); // 获取客户端地址
            clientPort = receivePacket.getPort(); // 获取客户端端口
            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws Exception {
        boolean isExit = false;
        while (!isExit) {
            System.out.println();
            System.out.println("---------------------------------");
            System.out.println("1. 服务器模式 上传文件");
            System.out.println("2. 客户端模式 下载文件");
            System.out.println("3. 退出应用");
            System.out.println("---------------------------------");
            System.out.print("请输入数字，选择模式: ");
            // Scanner in = new Scanner(System.in);
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

    private void downloadMode() throws Exception {
        boolean isExit = false;
        while (!isExit) {
            clientSocket = new DatagramSocket(); // 创建接收方UDP套接字
            System.out.println();
            System.out.println("进入客户端下载模式，以下是可输入的命令");
            System.out.println("---------------------------------");
            System.out.println(AppConfig.TIME_PROMPT + ": " + AppConfig.TIME_PROMPT_HELP);
            System.out.println(AppConfig.GBN_PROMPT + ": " + AppConfig.GBN_PROMPT_HELP);
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
                            receiver = new Receiver(clientSocket, serverAddress, dstPort, GBNConfig.SEQ_BITS,
                                CommonConfig.RECEIVER_PACKET_LOSS_RATE);
                        }
                        String fileName = args[1];
                        receiveFile(fileName);
                    }
                }
            }
        }
    }

    private void uploadMode() throws Exception {
        serverSocket = new DatagramSocket(srcPort);
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
                            System.out.println("客户端请求测试GBN协议...");
                            sender = new Sender(serverSocket, clientAddress, clientPort, GBNConfig.WINDOW_SIZE,
                                GBNConfig.SEQ_BITS, CommonConfig.SENDER_PACKET_LOSS_RATE, CommonConfig.TIMEOUT);
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

    private void sendFile(String fileName) throws Exception {
        log.info("开始发送文件...");
        fileName = getFilePath("upload", fileName);
        sender.sendFile(fileName);
        log.info("文件发送完毕！");
    }

    private void receiveFile(String fileName) throws Exception {
        log.info("客户端开始接收文件...");
        fileName = getFilePath("download", fileName);
        receiver.receiveData(fileName);
        log.info("客户端文件接收完毕！");
    }

    private String receiveStringAsClient() {
        byte[] receiveBuffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        try {
            clientSocket.receive(receivePacket);
            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
