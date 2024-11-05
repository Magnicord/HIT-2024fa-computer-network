#define _GNU_SOURCE

#include <arpa/inet.h>
#include <linux/if_packet.h>
#include <net/if.h>
#include <netinet/if_ether.h>
#include <netinet/ip.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <time.h>
#include <unistd.h>

#define BUFFER_SIZE 65536  // 定义缓冲区大小为 65536 字节

// 定义路由表项结构体
struct route_entry {
    uint32_t dest;             // 目的地址
    uint32_t gateway;          // 网关地址
    uint32_t netmask;          // 子网掩码
    char interface[IFNAMSIZ];  // 接口名称
};

// 定义路由表，包含一个路由表项
struct route_entry route_table[1];

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
    addr.s_addr = ip_addr;                               // 将 IP 地址赋值给 in_addr 结构体
    inet_ntop(AF_INET, &addr, ip_str, INET_ADDRSTRLEN);  // 将 IP 地址转换为字符串形式
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
    unsigned int sum = 0;     // 初始化校验和为 0
    unsigned short result;    // 存储校验和结果

    // 每次处理两个字节，累加到校验和
    for (sum = 0; len > 1; len -= 2) sum += *buf++;
    if (len == 1) sum += *(unsigned char *)buf;  // 如果有剩余一个字节，累加到校验和
    sum = (sum >> 16) + (sum & 0xFFFF);          // 将高 16 位和低 16 位相加
    sum += (sum >> 16);                          // 如果还有进位，再加一次
    result = ~sum;                               // 取反得到校验和
    return result;                               // 返回校验和
}

/**
 * 查找路由表以确定目的 IP 地址的路由
 *
 * @param dest_ip 目的 IP 地址
 * @return 指向匹配的路由表项的指针，如果没有匹配项则返回 NULL
 */
struct route_entry *lookup_route(uint32_t dest_ip) {
    char ip_str[32];  // 存储 IP 地址字符串的缓冲区

    // 遍历路由表
    for (int i = 0; i < route_table_size; i++) {
        // convert_to_ip_string(dest_ip, ip_str);
        // printf("IP Address: %s\n", ip_str);

        // convert_to_ip_string(route_table[i].dest, ip_str);
        // printf("IP Address: %s\n", ip_str);

        // 判断目的 IP 地址是否匹配路由表中的某一项
        if ((dest_ip & route_table[i].netmask) == (route_table[i].dest & route_table[i].netmask)) {
            convert_to_ip_string(dest_ip, ip_str);  // 将目的 IP 地址转换为字符串形式
            printf("-------IP Address: %s\n", ip_str);

            convert_to_ip_string(route_table[i].dest,
                                 ip_str);  // 将路由表中的目的地址转换为字符串形式
            printf("--------IP Address: %s\n", ip_str);
            return &route_table[i];  // 返回匹配的路由表项
        }
    }
    return NULL;  // 如果没有匹配项，返回 NULL
}

/**
 * 初始化路由表
 */
void initialize_route_table() {
    route_table[0].dest = inet_addr("192.168.80.2");       // 设置目的地址
    route_table[0].gateway = inet_addr("192.168.80.130");  // 设置网关地址
    route_table[0].netmask = inet_addr("255.255.255.0");   // 设置子网掩码
    strcpy(route_table[0].interface, "ens37");             // 设置接口名称
}

// 主函数
int main() {
    int sockfd;             // 套接字文件描述符
    struct sockaddr saddr;  // 定义 sockaddr 结构体变量，表示源地址
    unsigned char *buffer = (unsigned char *)malloc(BUFFER_SIZE);  // 分配缓冲区内存

    initialize_route_table();  // 初始化路由表

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
        if (data_size == 0) continue;  // 如果接收到的数据包大小为 0，继续下一次循环
        struct ethhdr *eth_header =
            (struct ethhdr *)buffer;  // 定义以太网头部指针，指向缓冲区的开始位置

        struct iphdr *ip_header =
            (struct iphdr
                 *)(buffer +
                    sizeof(struct ethhdr));  // 定义 IP 头部指针，指向缓冲区中以太网头部之后的位置
        struct route_entry *route =
            lookup_route(ip_header->daddr);  // 查找路由表以确定目的 IP 地址的路由

        if (route == NULL) {
            // fprintf(stderr, "No route to host\n");
            continue;
        }
        char ip_s[32], ip_d[32];
        convert_to_ip_string(ip_header->saddr, ip_s);  // 将源 IP 地址转换为字符串形式
        convert_to_ip_string(ip_header->daddr, ip_d);  // 将目的 IP 地址转换为字符串形式

        printf("Captured packet from %s to %s\n", ip_s,
               ip_d);  // 打印捕获的数据包信息，包括源 IP 地址和目的 IP 地址

        // 修改 TTL
        ip_header->ttl -= 1;   // 将 TTL 减 1
        ip_header->check = 0;  // 将校验和字段置 0
        ip_header->check =
            checksum((unsigned short *)ip_header, ip_header->ihl * 4);  // 计算新的校验和

        // 发送数据包到目的主机
        struct ifreq ifr, ifr_mac;
        struct sockaddr_ll dest;
        // 获取网卡接口索引
        memset(&ifr, 0, sizeof(ifr));
        snprintf(ifr.ifr_name, sizeof(ifr.ifr_name), route->interface);
        if (ioctl(sockfd, SIOCGIFINDEX, &ifr) < 0) {
            perror("ioctl");
            return 1;
        }
        // 获取网卡接口 MAC 地址
        memset(&ifr_mac, 0, sizeof(ifr_mac));
        snprintf(ifr_mac.ifr_name, sizeof(ifr_mac.ifr_name), route->interface);
        if (ioctl(sockfd, SIOCGIFHWADDR, &ifr_mac) < 0) {
            perror("ioctl");
            return 1;
        }
        // 设置目标 MAC
        // 地址（假设目标地址已知，此处做了简化处理，实际上，如果查找路由表后，存在“下一跳”，应该利用
        // ARP 协议获得 route->gateway 的 MAC 地址，如果是“直接交付”的话，也应使用 ARP
        // 协议获得目的主机的 MAC 地址。）
        unsigned char target_mac[ETH_ALEN] = {0x00, 0x0c, 0x29,
                                              0x5c, 0x8d, 0xee};  // 替换为实际的目标 MAC 地址
        memset(&dest, 0, sizeof(dest));
        dest.sll_ifindex = ifr.ifr_ifindex;
        dest.sll_halen = ETH_ALEN;
        memcpy(dest.sll_addr, target_mac, ETH_ALEN);
        // 构造新的以太网帧头
        memcpy(eth_header->h_dest, target_mac, ETH_ALEN);                    // 目标 MAC 地址
        memcpy(eth_header->h_source, ifr_mac.ifr_hwaddr.sa_data, ETH_ALEN);  // 源 MAC 地址
        eth_header->h_proto = htons(ETH_P_IP);  // 以太网类型为 IP
        printf("Interface name: %s, index: %d\n", ifr.ifr_name, ifr.ifr_ifindex);

        if (sendto(sockfd, buffer, data_size, 0, (struct sockaddr *)&dest, sizeof(dest)) < 0) {
            perror("Sendto error");  // 如果发送数据失败，输出错误信息
            return 1;                // 返回 1 表示程序异常终止
        }
    }
    close(sockfd);  // 关闭套接字
    free(buffer);   // 释放缓冲区内存
    return 0;       // 返回 0 表示程序正常终止
}
