#include <arpa/inet.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <time.h>  // 添加时间库头文件
#include <unistd.h>

#define SRC_PORT 12345
#define DEST_PORT 54321
#define DST_IP "172.20.10.8"

int main() {
    int sockfd;  // 套接字文件描述符
    struct sockaddr_in src_addr, dest_addr,
        my_addr;  // 定义三个 sockaddr_in 结构体变量，分别表示源地址、目标地址和本地地址
    char buffer[1024];              // 数据缓冲区，大小为 1024 字节
    socklen_t addr_len;             // 地址长度
    char src_ip[INET_ADDRSTRLEN];   // 源 IP 字符串
    char dest_ip[INET_ADDRSTRLEN];  // 目的 IP 字符串

    // 创建 UDP 套接字
    if ((sockfd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0) {
        perror("socket");  // 如果创建套接字失败，输出错误信息
        return 1;          // 返回 1 表示程序异常终止
    }

    // 本地地址配置
    my_addr.sin_family = AF_INET;          // 地址族为 IPv4
    my_addr.sin_port = htons(SRC_PORT);    // 将端口号转换为网络字节序
    my_addr.sin_addr.s_addr = INADDR_ANY;  // 监听所有本地 IP 地址

    // 绑定套接字到本地地址
    if (bind(sockfd, (struct sockaddr *)&my_addr, sizeof(my_addr)) < 0) {
        perror("bind");  // 如果绑定失败，输出错误信息
        return 1;        // 返回 1 表示程序异常终止
    }

    // 修改目标地址为接收程序主机的 IP 地址
    dest_addr.sin_family = AF_INET;                 // 地址族为 IPv4
    dest_addr.sin_port = htons(DEST_PORT);          // 将目标端口号转换为网络字节序
    dest_addr.sin_addr.s_addr = inet_addr(DST_IP);  // 替换为接收程序主机的实际 IP 地址

    while (1) {
        // 接收数据报
        addr_len = sizeof(src_addr);        // 初始化源地址长度
        memset(buffer, 0, sizeof(buffer));  // 清空缓冲区
        if (recvfrom(sockfd, buffer, sizeof(buffer), 0, (struct sockaddr *)&src_addr, &addr_len) <
            0) {
            perror("recvfrom");  // 如果接收数据失败，输出错误信息
            return 1;            // 返回 1 表示程序异常终止
        }

        // 获取当前时间
        time_t now = time(NULL);
        struct tm *t = localtime(&now);
        char time_str[64];
        strftime(time_str, sizeof(time_str), "%Y-%m-%d %H:%M:%S", t);

        // 获取源 IP 地址
        inet_ntop(AF_INET, &src_addr.sin_addr, src_ip, sizeof(src_ip));
        // 获取目的 IP 地址
        inet_ntop(AF_INET, &dest_addr.sin_addr, dest_ip, sizeof(dest_ip));

        // 打印日志信息
        printf("[%s] 接收到数据报：\n", time_str);
        printf("源 IP：%s，源端口：%d\n", src_ip, ntohs(src_addr.sin_port));
        printf("目的 IP：%s，目的端口：%d\n", dest_ip, DEST_PORT);
        printf("消息内容：%s\n", buffer);

        // 转发数据报
        if (sendto(sockfd, buffer, strlen(buffer), 0, (struct sockaddr *)&dest_addr,
                   sizeof(dest_addr)) < 0) {
            perror("sendto");  // 如果发送数据报失败，输出错误信息
            return 1;          // 返回 1 表示程序异常终止
        }

        printf("已转发数据报。\n");
    }

    close(sockfd);  // 关闭套接字
    return 0;       // 返回 0 表示程序正常终止
}
