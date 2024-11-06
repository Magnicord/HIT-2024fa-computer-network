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

#define UDP_SRC_PORT 12345
#define UDP_DST_PORT 12345

#define SRC_IP "172.20.10.6"

#define ETHER_TYPE 0x0800
#define DEST_MAC0 0x00
#define DEST_MAC1 0x0c
#define DEST_MAC2 0x29
#define DEST_MAC3 0x74
#define DEST_MAC4 0x21
#define DEST_MAC5 0xb5

#define BUFFER_SIZE 1518

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
    int sockfd;  // 套接字文件描述符
    struct ifreq if_idx,
        if_mac;  // 定义两个 ifreq 结构体变量，分别表示接口索引和接口 MAC 地址
    struct sockaddr_ll
        socket_address;  // 定义 sockaddr_ll 结构体变量，表示 socket 地址
    char buffer[BUFFER_SIZE];  // 数据缓冲区，大小为 1518 字节

    // 创建原始套接字
    if ((sockfd = socket(AF_PACKET, SOCK_RAW, htons(ETH_P_ALL))) == -1) {
        perror("socket");  // 如果创建套接字失败，输出错误信息
        return 1;          // 返回 1 表示程序异常终止
    }

    // 获取接口索引
    memset(&if_idx, 0, sizeof(struct ifreq));  // 将 ifreq 结构体清零
    strncpy(if_idx.ifr_name, "ens33", IFNAMSIZ - 1);  // 设置接口名称
    if (ioctl(sockfd, SIOCGIFINDEX, &if_idx) < 0) {
        perror("SIOCGIFINDEX");  // 如果获取接口索引失败，输出错误信息
        return 1;                // 返回 1 表示程序异常终止
    }

    // 获取接口 MAC 地址
    memset(&if_mac, 0, sizeof(struct ifreq));  // 将 ifreq 结构体清零
    strncpy(if_mac.ifr_name, "ens33", IFNAMSIZ - 1);  // 设置接口名称
    if (ioctl(sockfd, SIOCGIFHWADDR, &if_mac) < 0) {
        perror("SIOCGIFHWADDR");  // 如果获取接口 MAC 地址失败，输出错误信息
        return 1;                 // 返回 1 表示程序异常终止
    }

    // 让用户输入目标 IP 地址
    char dst_ip[INET_ADDRSTRLEN];
    printf("请输入目标 IP 地址（如 172.20.10.8）：");
    scanf("%s", dst_ip);

    // 让用户输入消息内容
    char msg[1024];
    printf("请输入要发送的消息：");
    scanf("%s", msg);

    // 构造以太网头部
    struct ether_header *eh = (struct ether_header *)buffer;
    memcpy(eh->ether_shost, if_mac.ifr_hwaddr.sa_data,
           ETH_ALEN);  // 源 MAC 地址

    // 设置目的 MAC 地址为路由器的 MAC 地址
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
    iph->tot_len = htons(sizeof(struct iphdr) + sizeof(struct udphdr) +
                         strlen(msg));  // 总长度
    iph->id = htonl(54321);             // 标识
    iph->frag_off = 0;                  // 分片偏移
    iph->ttl = 255;                     // 生存时间
    iph->protocol = IPPROTO_UDP;        // 协议类型为 UDP
    iph->check = 0;                     // 校验和初始化为 0
    iph->saddr = inet_addr(SRC_IP);     // 源 IP 地址
    iph->daddr = inet_addr(dst_ip);     // 目标 IP 地址
    iph->check = checksum((unsigned short *)iph,
                          sizeof(struct iphdr));  // 计算 IP 头部校验和

    // 构造 UDP 头部
    struct udphdr *udph =
        (struct udphdr *)(buffer + sizeof(struct ether_header) +
                          sizeof(struct iphdr));
    udph->source = htons(UDP_SRC_PORT);                      // 源端口号
    udph->dest = htons(UDP_DST_PORT);                        // 目标端口号
    udph->len = htons(sizeof(struct udphdr) + strlen(msg));  // UDP 数据包长度
    udph->check = 0;  // UDP 校验和可选，初始化为 0

    // 填充数据
    char *data = buffer + sizeof(struct ether_header) + sizeof(struct iphdr) +
                 sizeof(struct udphdr);
    strcpy(data, msg);  // 将消息复制到数据部分

    // 设置 socket 地址结构
    socket_address.sll_ifindex = if_idx.ifr_ifindex;  // 接口索引
    socket_address.sll_halen = ETH_ALEN;              // 地址长度
    memcpy(socket_address.sll_addr, eh->ether_dhost,
           ETH_ALEN);  // 目标 MAC 地址

    // 计算数据包的总长度
    int len = sizeof(struct ether_header) + sizeof(struct iphdr) +
              sizeof(struct udphdr) + strlen(msg);

    // 获取源 MAC 地址和目的 MAC 地址的字符串形式
    char src_mac[18], dest_mac[18];
    snprintf(src_mac, sizeof(src_mac), "%02x:%02x:%02x:%02x:%02x:%02x",
             eh->ether_shost[0], eh->ether_shost[1], eh->ether_shost[2],
             eh->ether_shost[3], eh->ether_shost[4], eh->ether_shost[5]);

    snprintf(dest_mac, sizeof(dest_mac), "%02x:%02x:%02x:%02x:%02x:%02x",
             eh->ether_dhost[0], eh->ether_dhost[1], eh->ether_dhost[2],
             eh->ether_dhost[3], eh->ether_dhost[4], eh->ether_dhost[5]);

    // 打印发送的数据包信息
    printf("即将发送数据包：\n");
    printf("源 MAC 地址：%s\n", src_mac);
    printf("目的 MAC 地址：%s\n", dest_mac);
    printf("源 IP 地址：%s\n", SRC_IP);
    printf("目的 IP 地址：%s\n", dst_ip);
    printf("源端口：%d\n", UDP_SRC_PORT);
    printf("目的端口：%d\n", UDP_DST_PORT);
    printf("发送的消息内容：%s\n", msg);

    // 发送数据包
    if (sendto(sockfd, buffer, len, 0, (struct sockaddr *)&socket_address,
               sizeof(struct sockaddr_ll)) < 0) {
        perror("sendto");  // 如果发送数据包失败，输出错误信息
        return 1;          // 返回 1 表示程序异常终止
    }

    printf("数据包已发送到 %s\n", dst_ip);  // 打印发送成功的信息

    close(sockfd);  // 关闭套接字
    return 0;       // 返回 0 表示程序正常终止
}
