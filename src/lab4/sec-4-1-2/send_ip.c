#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#define DST_IP "172.20.10.7"

int main() {
    int sockfd;                    // 套接字文件描述符
    struct sockaddr_in dest_addr;  // 定义 sockaddr_in 结构体变量，表示目标地址
    char *message = "Hello, this is a UDP datagram!";  // 要发送的消息
    int port = 12345;                                  // 目标端口号

    // 创建 UDP 套接字
    if ((sockfd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0) {
        perror("socket");  // 如果创建套接字失败，输出错误信息
        return 1;          // 返回 1 表示程序异常终止
    }

    // 目标地址配置
    dest_addr.sin_family = AF_INET;                 // 地址族为 IPv4
    dest_addr.sin_port = htons(port);               // 将端口号转换为网络字节序
    dest_addr.sin_addr.s_addr = inet_addr(DST_IP);  // 目标 IP 地址

    // 发送数据报
    if (sendto(sockfd, message, strlen(message), 0, (struct sockaddr *)&dest_addr,
               sizeof(dest_addr)) < 0) {
        perror("sendto");  // 如果发送数据失败，输出错误信息
        return 1;          // 返回 1 表示程序异常终止
    }

    printf("Datagram sent.\n");  // 输出发送成功的信息

    close(sockfd);  // 关闭套接字
    return 0;       // 返回 0 表示程序正常终止
}
