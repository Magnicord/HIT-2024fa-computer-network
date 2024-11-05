#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#define DST_IP "172.20.10.8"

int main() {
    int sockfd;  // 套接字文件描述符
    struct sockaddr_in src_addr, dest_addr,
        my_addr;  // 定义三个 sockaddr_in 结构体变量，分别表示源地址、目标地址和本地地址
    char buffer[1024];      // 数据缓冲区，大小为 1024 字节
    socklen_t addr_len;     // 地址长度
    int src_port = 12345;   // 原始端口号
    int dest_port = 54321;  // 目标端口号（接收程序的端口号）

    // 创建 UDP 套接字
    if ((sockfd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0) {
        perror("socket");  // 如果创建套接字失败，输出错误信息
        return 1;          // 返回 1 表示程序异常终止
    }

    // 本地地址配置
    my_addr.sin_family = AF_INET;          // 地址族为 IPv4
    my_addr.sin_port = htons(src_port);    // 将端口号转换为网络字节序
    my_addr.sin_addr.s_addr = INADDR_ANY;  // 监听所有本地 IP 地址

    // 绑定套接字到本地地址
    if (bind(sockfd, (struct sockaddr *)&my_addr, sizeof(my_addr)) < 0) {
        perror("bind");  // 如果绑定失败，输出错误信息
        return 1;        // 返回 1 表示程序异常终止
    }

    // 接收数据报
    addr_len = sizeof(src_addr);  // 初始化源地址长度
    if (recvfrom(sockfd, buffer, sizeof(buffer), 0, (struct sockaddr *)&src_addr, &addr_len) < 0) {
        perror("recvfrom");  // 如果接收数据失败，输出错误信息
        return 1;            // 返回 1 表示程序异常终止
    }
    printf("Datagram received: %s\n", buffer);  // 打印接收到的数据报

    // 修改目标地址为接收程序主机的 IP 地址
    dest_addr.sin_family = AF_INET;                 // 地址族为 IPv4
    dest_addr.sin_port = htons(dest_port);          // 将目标端口号转换为网络字节序
    dest_addr.sin_addr.s_addr = inet_addr(DST_IP);  // 替换为接收程序主机的实际 IP 地址

    // 发送数据报
    if (sendto(sockfd, buffer, strlen(buffer), 0, (struct sockaddr *)&dest_addr,
               sizeof(dest_addr)) < 0) {
        perror("sendto");  // 如果发送数据报失败，输出错误信息
        return 1;          // 返回 1 表示程序异常终止
    }
    printf("Datagram forwarded.\n");  // 打印数据报转发成功的信息

    close(sockfd);  // 关闭套接字
    return 0;       // 返回 0 表示程序正常终止
}
