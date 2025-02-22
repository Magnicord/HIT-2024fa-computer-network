#include <arpa/inet.h>
#include <netinet/udp.h>  // 添加 UDP 头部的头文件
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#define DEST_PORT 12345  // 目标端口号
#define SRC_PORT 12345   // 源端口号

#define DEST_IP "192.168.2.2"                     // 目标 IP 地址
#define MESSAGE "Hello, this is a test message."  // 要发送的消息

int main() {
    int sockfd;                    // 套接字文件描述符
    struct sockaddr_in dest_addr;  // 定义 sockaddr_in 结构体变量，表示目的地址
    char buffer[1024];                       // 数据缓冲区
    socklen_t addr_len = sizeof(dest_addr);  // 地址长度

    // 创建 UDP 套接字
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        perror("Socket creation failed");  // 如果创建套接字失败，输出错误信息
        return 1;  // 返回 1，表示程序异常终止
    }

    // 设置目的地址
    memset(&dest_addr, 0, sizeof(dest_addr));  // 将目的地址结构体清零
    dest_addr.sin_family = AF_INET;            // 地址族为 IPv4
    dest_addr.sin_port = htons(DEST_PORT);  // 将端口号转换为网络字节序
    inet_pton(
        AF_INET, DEST_IP,
        &dest_addr.sin_addr);  // 将目标 IP
                               // 地址转换为网络字节序并赋值给目的地址结构体

    // 设置源地址（可选，根据需要绑定）
    struct sockaddr_in src_addr;
    memset(&src_addr, 0, sizeof(src_addr));  // 将源地址结构体清零
    src_addr.sin_family = AF_INET;           // 地址族为 IPv4
    src_addr.sin_addr.s_addr = INADDR_ANY;   // 任意本地地址
    src_addr.sin_port = htons(SRC_PORT);     // 源端口号

    // 绑定源端口（可选，如果不绑定，系统会自动分配端口）
    if (bind(sockfd, (struct sockaddr *)&src_addr, sizeof(src_addr)) < 0) {
        perror("Bind failed");  // 如果绑定失败，输出错误信息
        return 1;               // 返回 1，表示程序异常终止
    }

    // 发送数据包
    if (sendto(sockfd, MESSAGE, strlen(MESSAGE), 0,
               (struct sockaddr *)&dest_addr, sizeof(dest_addr)) < 0) {
        perror("Sendto failed");  // 如果发送数据包失败，输出错误信息
        return 1;                 // 返回 1，表示程序异常终止
    }

    printf("Message sent to %s:%d\n", DEST_IP,
           DEST_PORT);  // 打印发送成功的信息，包括目标 IP 地址和端口号

    // 接收响应（可选，根据需要实现双向通信）
    struct sockaddr_in recv_addr;
    socklen_t recv_addr_len = sizeof(recv_addr);
    int recv_len = recvfrom(sockfd, buffer, sizeof(buffer) - 1, 0,
                            (struct sockaddr *)&recv_addr, &recv_addr_len);
    if (recv_len < 0) {
        perror("Recvfrom failed");  // 如果接收数据失败，输出错误信息
        // 可以选择继续或退出，这里选择继续
    } else {
        buffer[recv_len] = '\0';  // 将接收到的数据转换为字符串形式
        printf("Received message from %s:%d: %s\n",
               inet_ntoa(recv_addr.sin_addr), ntohs(recv_addr.sin_port),
               buffer);  // 打印接收到的数据和来源信息
    }

    close(sockfd);  // 关闭套接字
    return 0;       // 返回 0，表示程序正常终止
}
