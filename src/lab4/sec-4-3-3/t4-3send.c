#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#define DEST_IP "192.168.80.2"                    // 目标 IP 地址
#define DEST_PORT 12345                           // 目标端口号
#define MESSAGE "Hello, this is a test message."  // 要发送的消息

// 主函数
int main() {
    int sockfd;                    // 套接字文件描述符
    struct sockaddr_in dest_addr;  // 定义 sockaddr_in 结构体变量，表示目的地址

    // 创建 UDP 套接字
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        perror("Socket creation failed");  // 如果创建套接字失败，输出错误信息
        return 1;                          // 返回 1 表示程序异常终止
    }

    // 设置目的地址
    memset(&dest_addr, 0, sizeof(dest_addr));  // 将目的地址结构体清零
    dest_addr.sin_family = AF_INET;            // 地址族为 IPv4
    dest_addr.sin_port = htons(DEST_PORT);     // 将端口号转换为网络字节序
    inet_pton(AF_INET, DEST_IP,
              &dest_addr.sin_addr);  // 将目标 IP 地址转换为网络字节序并赋值给目的地址结构体

    // 发送数据包
    if (sendto(sockfd, MESSAGE, strlen(MESSAGE), 0, (struct sockaddr *)&dest_addr,
               sizeof(dest_addr)) < 0) {
        perror("Sendto failed");  // 如果发送数据包失败，输出错误信息
        return 1;                 // 返回 1 表示程序异常终止
    }

    printf("Message sent to %s:%d\n", DEST_IP,
           DEST_PORT);  // 打印发送成功的信息，包括目标 IP 地址和端口号

    close(sockfd);  // 关闭套接字
    return 0;       // 返回 0 表示程序正常终止
}
