#define _GNU_SOURCE

#include <arpa/inet.h>
#include <linux/if_packet.h>
#include <net/if.h>
#include <netinet/ether.h>
#include <netinet/ip.h>
#include <netinet/udp.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <unistd.h>

#define DEST_MAC0 0x00
#define DEST_MAC1 0x0c
#define DEST_MAC2 0x29
#define DEST_MAC3 0x83
#define DEST_MAC4 0xbd
#define DEST_MAC5 0xdf
#define ETHER_TYPE 0x0800
#define BUFFER_SIZE 1518
#define UDP_SRC_PORT 12345
#define UDP_DST_PORT 12345

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
    for (sum = 0; len > 1; len -= 2) {
        sum += *buf++;
    }

    // 如果有剩余一个字节，累加到校验和
    if (len == 1) {
        sum += *(unsigned char *)buf;
    }

    // 将高 16 位和低 16 位相加
    sum = (sum >> 16) + (sum & 0xFFFF);
    // 如果还有进位，再加一次
    sum += (sum >> 16);
    // 取反得到校验和
    result = ~sum;

    // 返回校验和
    return result;
}

// 主函数
int main() {
    int sockfd;  // 套接字文件描述符
    struct ifreq if_idx, if_mac;  // 定义两个 ifreq 结构体变量，分别表示接口索引和接口 MAC 地址
    struct sockaddr_ll socket_address;  // 定义 sockaddr_ll 结构体变量，表示 socket 地址
    char buffer[BUFFER_SIZE];           // 数据缓冲区，大小为 1518 字节
    char msg[] = "Hello, this is a test message.111111111";  // 要发送的消息

    // 创建原始套接字
    if ((sockfd = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_ALL))) == -1) {
        perror("socket");  // 如果创建套接字失败，输出错误信息
        return 1;          // 返回 1 表示程序异常终止
    }

    // 获取接口索引
    memset(&if_idx, 0, sizeof(struct ifreq));         // 将 ifreq 结构体清零
    strncpy(if_idx.ifr_name, "ens33", IFNAMSIZ - 1);  // 设置接口名称
    if (ioctl(sockfd, SIOCGIFINDEX, &if_idx) < 0) {
        perror("SIOCGIFINDEX");  // 如果获取接口索引失败，输出错误信息
        return 1;                // 返回 1 表示程序异常终止
    }

    // 获取接口 MAC 地址
    memset(&if_mac, 0, sizeof(struct ifreq));         // 将 ifreq 结构体清零
    strncpy(if_mac.ifr_name, "ens33", IFNAMSIZ - 1);  // 设置接口名称
    if (ioctl(sockfd, SIOCGIFHWADDR, &if_mac) < 0) {
        perror("SIOCGIFHWADDR");  // 如果获取接口 MAC 地址失败，输出错误信息
        return 1;                 // 返回 1 表示程序异常终止
    }

    // 构造以太网头部
    struct ether_header *eh = (struct ether_header *)buffer;
    eh->ether_shost[0] = ((uint8_t *)&if_mac.ifr_hwaddr.sa_data)[0];
    eh->ether_shost[1] = ((uint8_t *)&if_mac.ifr_hwaddr.sa_data)[1];
    eh->ether_shost[2] = ((uint8_t *)&if_mac.ifr_hwaddr.sa_data)[2];
    eh->ether_shost[3] = ((uint8_t *)&if_mac.ifr_hwaddr.sa_data)[3];
    eh->ether_shost[4] = ((uint8_t *)&if_mac.ifr_hwaddr.sa_data)[4];
    eh->ether_shost[5] = ((uint8_t *)&if_mac.ifr_hwaddr.sa_data)[5];

    // 打印源 MAC 地址
    for (int i = 0; i < 6; i++) {
        printf("%02x ", eh->ether_shost[i]);
    }
    printf("\n");

    // 设置目标 MAC 地址
    eh->ether_dhost[0] = DEST_MAC0;
    eh->ether_dhost[1] = DEST_MAC1;
    eh->ether_dhost[2] = DEST_MAC2;
    eh->ether_dhost[3] = DEST_MAC3;
    eh->ether_dhost[4] = DEST_MAC4;
    eh->ether_dhost[5] = DEST_MAC5;
    eh->ether_type = htons(ETHER_TYPE);  // 设置以太网类型为 IP

    // 构造 IP 头部
    struct iphdr *iph = (struct iphdr *)(buffer + sizeof(struct ether_header));
    iph->ihl = 5;      // IP 头部长度
    iph->version = 4;  // IP 版本
    iph->tos = 0;      // 服务类型
    iph->tot_len = htons(sizeof(struct iphdr) + sizeof(struct udphdr) + strlen(msg));  // 总长度
    iph->id = htonl(54321);                                                            // 标识
    iph->frag_off = 0;                                                   // 分片偏移
    iph->ttl = 255;                                                      // 生存时间
    iph->protocol = IPPROTO_UDP;                                         // 协议类型为 UDP
    iph->check = 0;                                                      // 校验和初始化为 0
    iph->saddr = inet_addr("192.168.79.129");                            // 源 IP 地址
    iph->daddr = inet_addr("192.168.79.131");                            // 目标 IP 地址
    iph->check = checksum((unsigned short *)iph, sizeof(struct iphdr));  // 计算 IP 头部校验和

    // 构造 UDP 头部
    struct udphdr *udph =
        (struct udphdr *)(buffer + sizeof(struct ether_header) + sizeof(struct iphdr));
    udph->source = htons(UDP_SRC_PORT);                      // 源端口号
    udph->dest = htons(UDP_DST_PORT);                        // 目标端口号
    udph->len = htons(sizeof(struct udphdr) + strlen(msg));  // UDP 数据包长度
    udph->check = 0;                                         // UDP 校验和可选，初始化为 0

    // 填充数据
    char *data = (char *)(buffer + sizeof(struct ether_header) + sizeof(struct iphdr) +
                          sizeof(struct udphdr));
    strcpy(data, msg);  // 将消息复制到数据部分

    // 设置 socket 地址结构
    socket_address.sll_ifindex = if_idx.ifr_ifindex;  // 接口索引
    socket_address.sll_halen = ETH_ALEN;              // 地址长度
    socket_address.sll_addr[0] = DEST_MAC0;
    socket_address.sll_addr[1] = DEST_MAC1;
    socket_address.sll_addr[2] = DEST_MAC2;
    socket_address.sll_addr[3] = DEST_MAC3;
    socket_address.sll_addr[4] = DEST_MAC4;
    socket_address.sll_addr[5] = DEST_MAC5;

    // 发送数据包
    int len = sizeof(struct ether_header) + sizeof(struct iphdr) + sizeof(struct udphdr) +
              strlen(msg);    // 计算数据包的总长度
    printf("len=%d\n", len);  // 打印数据包的总长度

    // 使用 sendto 函数发送数据包
    if (sendto(sockfd, buffer, len, 0, (struct sockaddr *)&socket_address,
               sizeof(struct sockaddr_ll)) < 0) {
        perror("sendto");  // 如果发送数据包失败，输出错误信息
        return 1;          // 返回 1 表示程序异常终止
    }

    close(sockfd);  // 关闭套接字
    return 0;       // 返回 0 表示程序正常终止
}
