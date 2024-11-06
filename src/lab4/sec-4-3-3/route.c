#define _GNU_SOURCE

#include <arpa/inet.h>
#include <linux/if_packet.h>
#include <net/if.h>
#include <netinet/if_ether.h>
#include <netinet/ip.h>
#include <netinet/udp.h>  // 添加 UDP 头部的头文件
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <time.h>
#include <unistd.h>

#define BUFFER_SIZE 65536  // 定义缓冲区大小为 65536 字节
#define NODE_3_IP "192.168.2.2"
#define NODE_1_IP "192.168.1.2"
#define ENS36_IP "192.168.1.1"
#define ENS37_IP "192.168.2.1"
#define ENS36_NETMASK "255.255.255.0"
#define ENS37_NETMASK "255.255.255.0"

const unsigned char NODE_3_MAC_ADDR[] = {0x00, 0x0c, 0x29, 0x47, 0x72, 0x60};
const unsigned char NODE_1_MAC_ADDR[] = {0x00, 0x0c, 0x29, 0x1e, 0x75, 0x42};

// 定义路由表项结构体
struct route_entry {
    uint32_t dest;                         // 目的地址
    uint32_t gateway;                      // 网关地址
    uint32_t netmask;                      // 子网掩码
    char interface[IFNAMSIZ];              // 接口名称
    unsigned char next_hop_mac[ETH_ALEN];  // 下一跳的 MAC 地址
};

// 定义路由表，包含两个路由表项
struct route_entry route_table[2];

// 路由表大小
int route_table_size = sizeof(route_table) / sizeof(route_table[0]);

/**
 * 将 IP 地址转换为字符串形式
 *
 * @param ip_addr 需要转换的 IP 地址
 * @param ip_str 存储转换后字符串的缓冲区
 */
void convert_to_ip_string(uint32_t ip_addr, char *ip_str) {
    struct in_addr addr;
    addr.s_addr = ip_addr;  // 将 IP 地址赋值给 in_addr 结构体
    inet_ntop(AF_INET, &addr, ip_str,
              INET_ADDRSTRLEN);  // 将 IP 地址转换为字符串形式
}

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

/**
 * 查找路由表以确定目的 IP 地址的路由
 *
 * @param dest_ip 目的 IP 地址
 * @return 指向匹配的路由表项的指针，如果没有匹配项则返回 NULL
 */
struct route_entry *lookup_route(uint32_t dest_ip) {
    // 遍历路由表
    for (int i = 0; i < route_table_size; i++) {
        // 判断目的 IP 地址是否匹配路由表中的某一项
        if ((dest_ip & route_table[i].netmask) ==
            (route_table[i].dest & route_table[i].netmask)) {
            return &route_table[i];  // 返回匹配的路由表项
        }
    }
    return NULL;  // 如果没有匹配项，返回 NULL
}

/**
 * 初始化路由表
 */
void initialize_route_table() {
    // 路由到 192.168.2.0/24 网段
    route_table[0].dest = inet_addr("192.168.2.0");  // 目的网络地址
    route_table[0].gateway = inet_addr("0.0.0.0");   // 网关地址（直连）
    route_table[0].netmask = inet_addr(ENS37_NETMASK);  // 子网掩码
    strcpy(route_table[0].interface, "ens37");          // 接口名称
    memcpy(route_table[0].next_hop_mac, NODE_3_MAC_ADDR,
           ETH_ALEN);  // 下一跳 MAC 地址

    // 路由到 192.168.1.0/24 网段
    route_table[1].dest = inet_addr("192.168.1.0");  // 目的网络地址
    route_table[1].gateway = inet_addr("0.0.0.0");   // 网关地址（直连）
    route_table[1].netmask = inet_addr(ENS36_NETMASK);  // 子网掩码
    strcpy(route_table[1].interface, "ens36");          // 接口名称
    memcpy(route_table[1].next_hop_mac, NODE_1_MAC_ADDR,
           ETH_ALEN);  // 下一跳 MAC 地址
}

int main() {
    int sockfd;             // 套接字文件描述符
    struct sockaddr saddr;  // 定义 sockaddr 结构体变量，表示源地址
    unsigned char *buffer =
        (unsigned char *)malloc(BUFFER_SIZE);  // 分配缓冲区内存

    initialize_route_table();  // 初始化路由表

    sockfd = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_IP));  // 创建原始套接字
    if (sockfd < 0) {
        perror("Socket creation failed");  // 如果创建套接字失败，输出错误信息
        return 1;  // 返回 1，表示程序异常终止
    }
    while (1) {
        int saddr_len = sizeof(saddr);  // 初始化源地址长度
        int data_size = recvfrom(sockfd, buffer, BUFFER_SIZE, 0, &saddr,
                                 (socklen_t *)&saddr_len);  // 接收数据包
        if (data_size < 0) {
            perror("Recvfrom error");  // 如果接收数据失败，输出错误信息
            return 1;                  // 返回 1，表示程序异常终止
        }
        if (data_size == 0)
            continue;  // 如果接收到的数据包大小为 0，继续下一次循环

        struct ethhdr *eth_header = (struct ethhdr *)
            buffer;  // 定义以太网头部指针，指向缓冲区的开始位置
        struct iphdr *ip_header =
            (struct iphdr
                 *)(buffer +
                    sizeof(
                        struct
                        ethhdr));  // 定义 IP
                                   // 头部指针，指向缓冲区中以太网头部之后的位置

        struct route_entry *route = lookup_route(
            ip_header->daddr);  // 查找路由表以确定目的 IP 地址的路由

        if (route == NULL) {
            continue;  // 继续下一次循环
        }

        // 获取源 MAC 地址和目的 MAC 地址
        char src_mac[18], dest_mac[18];
        snprintf(src_mac, sizeof(src_mac), "%02x:%02x:%02x:%02x:%02x:%02x",
                 eth_header->h_source[0], eth_header->h_source[1],
                 eth_header->h_source[2], eth_header->h_source[3],
                 eth_header->h_source[4], eth_header->h_source[5]);
        snprintf(dest_mac, sizeof(dest_mac), "%02x:%02x:%02x:%02x:%02x:%02x",
                 eth_header->h_dest[0], eth_header->h_dest[1],
                 eth_header->h_dest[2], eth_header->h_dest[3],
                 eth_header->h_dest[4], eth_header->h_dest[5]);

        // 获取源 IP 地址和目的 IP 地址
        char ip_s[INET_ADDRSTRLEN], ip_d[INET_ADDRSTRLEN];
        convert_to_ip_string(ip_header->saddr,
                             ip_s);  // 将源 IP 地址转换为字符串形式
        convert_to_ip_string(ip_header->daddr,
                             ip_d);  // 将目的 IP 地址转换为字符串形式

        // 获取源端口号和目的端口号
        uint16_t src_port = 0, dest_port = 0;
        if (ip_header->protocol == IPPROTO_UDP) {
            struct udphdr *udp_header =
                (struct udphdr *)(buffer + sizeof(struct ethhdr) +
                                  ip_header->ihl * 4);
            src_port = ntohs(udp_header->source);
            dest_port = ntohs(udp_header->dest);
        } else if (ip_header->protocol == IPPROTO_TCP) {
            // 可选：处理 TCP 协议
        }

        // 获取 TTL 值
        int ttl = ip_header->ttl;

        // 获取当前系统时间并设置为北京时间
        time_t rawtime;
        struct tm *timeinfo;
        char time_str[100];
        time(&rawtime);  // 获取当前时间

        // 设置时区为北京时间
        setenv("TZ", "Asia/Shanghai", 1);  // 设置环境变量 TZ 为北京时区
        tzset();                           // 初始化时区配置

        timeinfo = localtime(&rawtime);  // 将时间转换为本地时间（北京时间）
        // 格式化时间字符串
        strftime(time_str, sizeof(time_str), "%Y-%m-%d %H:%M:%S", timeinfo);

        // 打印日志信息
        printf("[%s] 接收到数据包：\n", time_str);
        printf("源 MAC 地址：%s\n", src_mac);
        printf("目的 MAC 地址：%s\n", dest_mac);
        printf("源 IP 地址：%s\n", ip_s);
        printf("目的 IP 地址：%s\n", ip_d);
        printf("TTL：%d\n", ttl);
        printf("源端口号：%d\n", src_port);
        printf("目的端口号：%d\n", dest_port);

        // 修改 TTL
        ip_header->ttl -= 1;   // 将 TTL 减 1
        ip_header->check = 0;  // 将校验和字段置 0
        ip_header->check = checksum((unsigned short *)ip_header,
                                    ip_header->ihl * 4);  // 计算新的校验和

        // 获取接口信息
        struct ifreq ifr, ifr_mac;
        struct sockaddr_ll dest;
        // 获取网卡接口索引
        memset(&ifr, 0, sizeof(ifr));
        snprintf(ifr.ifr_name, sizeof(ifr.ifr_name), "%s", route->interface);
        if (ioctl(sockfd, SIOCGIFINDEX, &ifr) < 0) {
            perror("ioctl SIOCGIFINDEX failed");  // 输出错误信息
            return 1;  // 返回 1，表示程序异常终止
        }
        // 获取网卡接口 MAC 地址
        memset(&ifr_mac, 0, sizeof(ifr_mac));
        snprintf(ifr_mac.ifr_name, sizeof(ifr_mac.ifr_name), "%s",
                 route->interface);
        if (ioctl(sockfd, SIOCGIFHWADDR, &ifr_mac) < 0) {
            perror("ioctl SIOCGIFHWADDR failed");  // 输出错误信息
            return 1;  // 返回 1，表示程序异常终止
        }
        // 设置目标 MAC 地址
        memcpy(dest.sll_addr, route->next_hop_mac, ETH_ALEN);
        dest.sll_ifindex = ifr.ifr_ifindex;  // 接口索引
        dest.sll_halen = ETH_ALEN;           // 地址长度

        // 构造新的以太网帧头
        memcpy(eth_header->h_dest, route->next_hop_mac,
               ETH_ALEN);  // 目标 MAC 地址
        memcpy(eth_header->h_source, ifr_mac.ifr_hwaddr.sa_data,
               ETH_ALEN);                       // 源 MAC 地址
        eth_header->h_proto = htons(ETH_P_IP);  // 以太网类型为 IP

        // 打印接口信息
        printf("Interface name: %s, index: %d\n", ifr.ifr_name,
               ifr.ifr_ifindex);

        // 发送数据包到下一跳
        if (sendto(sockfd, buffer, data_size, 0, (struct sockaddr *)&dest,
                   sizeof(dest)) < 0) {
            perror("Sendto error");  // 如果发送数据失败，输出错误信息
            return 1;                // 返回 1，表示程序异常终止
        }

        printf("数据包已转发到 %s\n", ip_d);  // 打印转发成功的信息

        printf(
            "----------------------------------------------------\n");  // 分隔符，便于阅读日志
    }
    close(sockfd);  // 关闭套接字
    free(buffer);   // 释放缓冲区内存
    return 0;       // 返回 0，表示程序正常终止
}
