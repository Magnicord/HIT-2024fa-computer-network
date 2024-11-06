#define _GNU_SOURCE

#include <arpa/inet.h>
#include <linux/if_packet.h>
#include <net/if.h>
#include <netinet/ether.h>
#include <netinet/if_ether.h>
#include <netinet/ip.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <time.h>
#include <unistd.h>

#define SRC_IP "172.20.10.6"
#define DST_IP "172.20.10.8"

#define DEST_MAC0 0x00
#define DEST_MAC1 0x0c
#define DEST_MAC2 0x29
#define DEST_MAC3 0x47
#define DEST_MAC4 0x72
#define DEST_MAC5 0x56

#define BUFFER_SIZE 65536  // 定义缓冲区大小为 65536 字节

/**
 * 计算校验和的函数
 *
 * @param b 指向需要计算校验和的数据缓冲区的指针
 * @param len 数据缓冲区的长度（以字节为单位）
 * @return 计算得到的校验和（无符号短整型）
 */
unsigned short checksum(void *b, int len) {
    unsigned short *buf = b;  // 将缓冲区指针转换为无符号短整型指针
    unsigned int sum = 0;   // 初始化校验和为 0
    unsigned short result;  // 存储校验和结果

    // 每次处理两个字节，累加到校验和
    for (sum = 0; len > 1; len -= 2) {
        sum += *buf++;
    }

    // 如果有剩余一个字节，累加到校验和
    if (len == 1) {
        sum += *(unsigned char *)buf;
    }
    sum = (sum >> 16) + (sum & 0xFFFF);  // 将高 16 位和低 16 位相加
    sum += (sum >> 16);                  // 如果还有进位，再加一次
    result = ~sum;                       // 取反得到校验和
    return result;                       // 返回校验和
}

int main() {
    int sockfd;             // 套接字文件描述符
    struct sockaddr saddr;  // 定义 sockaddr 结构体变量，表示源地址
    unsigned char *buffer =
        (unsigned char *)malloc(BUFFER_SIZE);  // 分配缓冲区内存
    sockfd = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_IP));  // 创建原始套接字
    if (sockfd < 0) {
        perror("Socket creation failed");  // 如果创建套接字失败，输出错误信息
        return 1;                          // 返回 1 表示程序异常终止
    }
    while (1) {
        int saddr_len = sizeof(saddr);  // 初始化源地址长度
        int data_size = recvfrom(sockfd, buffer, BUFFER_SIZE, 0, &saddr,
                                 (socklen_t *)&saddr_len);  // 接收数据包
        if (data_size < 0) {
            perror("Recvfrom error");  // 如果接收数据失败，输出错误信息
            return 1;                  // 返回 1 表示程序异常终止
        }
        // 定义以太网头部指针，指向缓冲区的开始位置
        struct ethhdr *eth_header = (struct ethhdr *)buffer;
        // 定义 IP 头部指针，指向缓冲区中以太网头部之后的位置
        struct iphdr *ip_header =
            (struct iphdr *)(buffer + sizeof(struct ethhdr));
        // 定义源 IP 地址和目标 IP 地址的字符串缓冲区
        char src_ip[INET_ADDRSTRLEN];
        char dest_ip[INET_ADDRSTRLEN];
        // 将源 IP 地址转换为字符串形式
        inet_ntop(AF_INET, &(ip_header->saddr), src_ip, INET_ADDRSTRLEN);
        // 将目标 IP 地址转换为字符串形式
        inet_ntop(AF_INET, &(ip_header->daddr), dest_ip, INET_ADDRSTRLEN);
        // 判断数据包的源 IP 地址和目标 IP 地址是否符合指定条件
        if (strcmp(src_ip, SRC_IP) == 0 && strcmp(dest_ip, DST_IP) == 0) {
            // 获取当前系统时间
            time_t rawtime;
            struct tm *timeinfo;
            char time_str[100];
            time(&rawtime);                  // 获取当前时间
            timeinfo = localtime(&rawtime);  // 将时间转换为本地时间
            // 格式化时间字符串
            strftime(time_str, sizeof(time_str), "%Y-%m-%d %H:%M:%S", timeinfo);
            // 打印捕获的数据包信息，包括时间、源 IP 地址和目标 IP 地址
            printf("[%s] Captured packet from %s to %s\n", time_str, src_ip,
                   dest_ip);
            // 修改 IP 头部的 TTL（生存时间）字段，减 1
            ip_header->ttl -= 1;
            ip_header->check = 0;  // 将校验和字段置 0
            // 总长度= IP 首部长度 + IP 数据长度
            // ip_header->tot_len = htons(20 + 8 + 30);
            // 计算新的校验和
            ip_header->check =
                checksum((unsigned short *)ip_header, ip_header->ihl * 4);
            // 发送数据包到目的主机
            struct ifreq ifr, ifr_mac;
            struct sockaddr_ll dest;
            // 获取网卡接口索引
            memset(&ifr, 0, sizeof(ifr));
            snprintf(ifr.ifr_name, sizeof(ifr.ifr_name), "ens33");
            if (ioctl(sockfd, SIOCGIFINDEX, &ifr) < 0) {
                perror("ioctl");  // 如果获取接口索引失败，输出错误信息
                return 1;  // 返回 1 表示程序异常终止
            }
            // 获取网卡接口 MAC 地址
            memset(&ifr_mac, 0, sizeof(ifr_mac));
            snprintf(ifr_mac.ifr_name, sizeof(ifr_mac.ifr_name), "ens33");
            if (ioctl(sockfd, SIOCGIFHWADDR, &ifr_mac) < 0) {
                perror("ioctl");  // 如果获取接口 MAC 地址失败，输出错误信息
                return 1;  // 返回 1 表示程序异常终止
            }
            // 设置目标 MAC 地址（假设目标地址已知）
            unsigned char target_mac[ETH_ALEN] = {
                DEST_MAC0, DEST_MAC1, DEST_MAC2,
                DEST_MAC3, DEST_MAC4, DEST_MAC5};  // 替换为实际的目标 MAC 地址
            memset(&dest, 0, sizeof(dest));
            dest.sll_ifindex = ifr.ifr_ifindex;
            dest.sll_halen = ETH_ALEN;
            memcpy(dest.sll_addr, target_mac, ETH_ALEN);
            // 构造新的以太网帧头
            memcpy(eth_header->h_dest, target_mac, ETH_ALEN);  // 目标 MAC 地址
            memcpy(eth_header->h_source, ifr_mac.ifr_hwaddr.sa_data,
                   ETH_ALEN);                       // 源 MAC 地址
            eth_header->h_proto = htons(ETH_P_IP);  // 以太网类型为 IP
            printf("Interface name: %s, index: %d\n", ifr.ifr_name,
                   ifr.ifr_ifindex);
            if (sendto(sockfd, buffer, data_size, 0, (struct sockaddr *)&dest,
                       sizeof(dest)) < 0) {
                perror("Sendto error");  // 如果发送数据失败，输出错误信息
                return 1;                // 返回 1 表示程序异常终止
            }
            printf("Datagram forwarded.\n");  // 打印数据包转发成功的信息
        } else {
            printf("Ignored packet from %s to %s\n", src_ip,
                   dest_ip);  // 打印忽略的数据包信息
        }
    }
    close(sockfd);  // 关闭套接字
    free(buffer);   // 释放缓冲区内存
    return 0;       // 返回 0 表示程序正常终止
}
