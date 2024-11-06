#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#define PORT 12345  // 定义端口号为 12345

int main() {
    int sockfd;  // 套接字文件描述符
    struct sockaddr_in server_addr,
        client_addr;  // 定义两个 sockaddr_in
                      // 结构体变量，分别表示服务器地址和客户端地址
    socklen_t addr_len =
        sizeof(client_addr);  // 地址长度，初始化为客户端地址的大小
    char buffer[1024];        // 数据缓冲区，大小为 1024 字节

    // 创建 UDP 套接字
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        perror("Socket creation failed");  // 如果创建套接字失败，输出错误信息
        return 1;                          // 返回 1 表示程序异常终止
    }

    // 绑定套接字到端口
    memset(&server_addr, 0, sizeof(server_addr));  // 将服务器地址结构体清零
    server_addr.sin_family = AF_INET;              // 地址族为 IPv4
    server_addr.sin_addr.s_addr = INADDR_ANY;  // 监听所有本地 IP 地址
    server_addr.sin_port = htons(PORT);  // 将端口号转换为网络字节序

    // 绑定套接字到本地地址
    if (bind(sockfd, (struct sockaddr *)&server_addr, sizeof(server_addr)) <
        0) {
        perror("Bind failed");  // 如果绑定失败，输出错误信息
        return 1;               // 返回 1 表示程序异常终止
    }

    // 接收数据包
    int recv_len = recvfrom(sockfd, buffer, sizeof(buffer) - 1, 0,
                            (struct sockaddr *)&client_addr, &addr_len);
    if (recv_len < 0) {
        perror("Recvfrom failed");  // 如果接收数据失败，输出错误信息
        return 1;                   // 返回 1 表示程序异常终止
    }

    buffer[recv_len] = '\0';  // 将接收到的数据转换为字符串形式
    printf("Received message: %s\n", buffer);  // 打印接收到的数据

    close(sockfd);  // 关闭套接字
    return 0;       // 返回 0 表示程序正常终止
}
