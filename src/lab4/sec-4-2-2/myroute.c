#define _GNU_SOURCE

#include <arpa/inet.h>
#include <linux/if_packet.h>
#include <net/if.h>
#include <netinet/ether.h>
#include <netinet/ip.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <time.h>  // 添加时间库头文件
#include <unistd.h>

#define SRC_IP "172.20.10.6"

#define BUFFER_SIZE 65536  // 定义缓冲区大小为 65536 字节

// 定义转发表项的数据结构
struct routing_table_entry {
    char dest_ip[INET_ADDRSTRLEN];         // 目标 IP 地址
    unsigned char next_hop_mac[ETH_ALEN];  // 下一跳的 MAC 地址
};

// 定义转发表（使用静态数组）
struct routing_table_entry routing_table[] = {
    // 接收主机 1
    {"172.20.10.8", {0x00, 0x0c, 0x29, 0x47, 0x72, 0x56}},
    // 接收主机 2
    {"172.20.10.9", {0x00, 0x0c, 0x29, 0xa6, 0x2c, 0xbc}},
    // 接收主机 3
    {"172.20.10.10", {0x00, 0x0c, 0x29, 0x77, 0xbf, 0x04}},
    // 可以在此添加更多转发表项
};

#define ROUTING_TABLE_SIZE \
    (sizeof(routing_table) / sizeof(struct routing_table_entry))

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
    while (len > 1) {
        sum += *buf++;
        len -= 2;
    }

    // 如果有剩余一个字节，累加到校验和
    if (len == 1) {
        sum += *(unsigned char *)buf;
    }

    // 将高 16 位和低 16 位相加，直到高位为 0
    sum = (sum >> 16) + (sum & 0xFFFF);
    sum += (sum >> 16);

    result = ~sum;  // 取反得到校验和
    return result;  // 返回校验和
}

int main() {
    int sockfd;             // 套接字文件描述符
    struct sockaddr saddr;  // 定义 sockaddr 结构体变量，表示源地址
    unsigned char *buffer =
        (unsigned char *)malloc(BUFFER_SIZE);  // 分配缓冲区内存

    // 创建原始套接字
    sockfd = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_IP));
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

        // 如果来源 IP 非 SRC_IP: 172.20.10.6，则忽略
        if (strcmp(src_ip, SRC_IP) != 0) {
            continue;  // 跳过该数据包
        }

        // 定义源 MAC 地址和目的 MAC 地址的字符串缓冲区
        char src_mac[18];
        char dest_mac[18];

        // 将源 MAC 地址转换为字符串形式
        snprintf(src_mac, sizeof(src_mac), "%02x:%02x:%02x:%02x:%02x:%02x",
                 eth_header->h_source[0], eth_header->h_source[1],
                 eth_header->h_source[2], eth_header->h_source[3],
                 eth_header->h_source[4], eth_header->h_source[5]);

        // 将目的 MAC 地址转换为字符串形式
        snprintf(dest_mac, sizeof(dest_mac), "%02x:%02x:%02x:%02x:%02x:%02x",
                 eth_header->h_dest[0], eth_header->h_dest[1],
                 eth_header->h_dest[2], eth_header->h_dest[3],
                 eth_header->h_dest[4], eth_header->h_dest[5]);

        // 获取当前时间
        time_t now = time(NULL);
        struct tm *t = localtime(&now);
        char time_str[64];
        strftime(time_str, sizeof(time_str), "%Y-%m-%d %H:%M:%S", t);

        // 获取 TTL 值
        int ttl = ip_header->ttl;

        // 打印捕获的数据包信息，包括时间、源 MAC、目的 MAC、源 IP 地址、目标 IP
        // 地址、TTL
        printf("[%s] 接收到数据包：\n", time_str);
        printf("源 MAC 地址：%s\n", src_mac);
        printf("目的 MAC 地址：%s\n", dest_mac);
        printf("源 IP 地址：%s\n", src_ip);
        printf("目的 IP 地址：%s\n", dest_ip);
        printf("TTL：%d\n", ttl);

        // 检查 IP 头部的 TTL 是否大于 1
        if (ip_header->ttl <= 1) {
            printf("数据包 TTL 已过期，丢弃数据包。\n");
            continue;  // 丢弃数据包
        }

        // 递减 TTL（生存时间）字段
        ip_header->ttl -= 1;
        ip_header->check = 0;  // 将校验和字段置 0
        // 计算新的校验和
        ip_header->check =
            checksum((unsigned short *)ip_header, ip_header->ihl * 4);

        // 查找转发表，找到对应的下一跳 MAC 地址
        int found = 0;
        unsigned char next_hop_mac[ETH_ALEN];
        for (int i = 0; i < ROUTING_TABLE_SIZE; i++) {
            if (strcmp(dest_ip, routing_table[i].dest_ip) == 0) {
                memcpy(next_hop_mac, routing_table[i].next_hop_mac, ETH_ALEN);
                found = 1;
                break;
            }
        }

        if (!found) {
            printf("无法找到前往 %s 的路由，丢弃数据包。\n", dest_ip);
            continue;  // 如果没有找到路由，丢弃数据包
        }

        // 获取本地主机的接口信息
        struct ifreq ifr_index, ifr_mac;
        memset(&ifr_index, 0, sizeof(ifr_index));  // 清零结构体
        snprintf(ifr_index.ifr_name, sizeof(ifr_index.ifr_name),
                 "ens33");  // 替换为实际的接口名称
        if (ioctl(sockfd, SIOCGIFINDEX, &ifr_index) < 0) {
            perror("ioctl SIOCGIFINDEX");  // 如果获取接口索引失败，输出错误信息
            return 1;                      // 返回 1 表示程序异常终止
        }

        // 获取本地主机的 MAC 地址
        memset(&ifr_mac, 0, sizeof(ifr_mac));  // 清零结构体
        snprintf(ifr_mac.ifr_name, sizeof(ifr_mac.ifr_name),
                 "ens33");  // 替换为实际的接口名称
        if (ioctl(sockfd, SIOCGIFHWADDR, &ifr_mac) < 0) {
            perror("ioctl SIOCGIFHWADDR");  // 如果获取接口 MAC
                                            // 地址失败，输出错误信息
            return 1;                       // 返回 1 表示程序异常终止
        }

        // 设置以太网头部的目的 MAC 地址为下一跳 MAC 地址
        memcpy(eth_header->h_dest, next_hop_mac, ETH_ALEN);
        // 设置以太网头部的源 MAC 地址为路由器的 MAC 地址
        memcpy(eth_header->h_source, ifr_mac.ifr_hwaddr.sa_data, ETH_ALEN);

        // 设置目标地址结构体
        struct sockaddr_ll dest_addr;
        memset(&dest_addr, 0, sizeof(dest_addr));            // 清零结构体
        dest_addr.sll_ifindex = ifr_index.ifr_ifindex;       // 接口索引
        dest_addr.sll_halen = ETH_ALEN;                      // 地址长度
        memcpy(dest_addr.sll_addr, next_hop_mac, ETH_ALEN);  // 目标 MAC 地址

        // 发送数据包到下一跳
        if (sendto(sockfd, buffer, data_size, 0, (struct sockaddr *)&dest_addr,
                   sizeof(dest_addr)) < 0) {
            perror("Sendto error");  // 如果发送数据失败，输出错误信息
            return 1;                // 返回 1 表示程序异常终止
        }

        printf("数据包已转发至 %s。\n", dest_ip);  // 打印数据包转发成功的信息
    }

    close(sockfd);  // 关闭套接字
    free(buffer);   // 释放缓冲区内存
    return 0;       // 返回 0 表示程序正常终止
}
