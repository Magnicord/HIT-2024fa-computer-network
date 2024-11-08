# HIT 2024 Fall Computer Network

哈尔滨工业大学 2024 秋计算机网络实验

## 实验概览

1. lab1: HTTP 代理服务器的设计与实现
2. lab2: 可靠数据传输协议-GBN 协议的设计与实现
3. lab3: 利用 Wireshark 进行协议分析
4. lab4: IP 数据报的转发及收发
5. lab5: 简单网络组建及配置

## 实验指导手册

位于 `docs` 目录下。其中， `《计算机网络》实验指导书(2023).pdf` 为旧版指导书。2024 秋的实验，除了 lab4 外，均摘自该指导书。接下来，对各个文件做简单的介绍:

- `《计算机网络》实验N.pdf`: 第 N 次实验需要验收的内容，摘自旧版指导书的若干实验
- `实验N.pptx`: 第 N 次实验课所用课件
- `实验N验收提纲`: 第 N 次实验验收提纲，部分老师采取学生录像验收，部分老师采取线下验收

对于 lab4，2024 秋做了更改，具体见 `docs/lab4`

> [!Note]
> 实验指导书中的选做内容，均为**必做**

## 实验源码

lab3 与 lab5 无代码实现。

对于 lab1 与 lab2，使用 Java 实现，实验环境如下:

- 操作系统: Windows 11 23H2
- 编程语言: Java 21
- IDE: IntelliJ IDEA 2024.2.4 (Ultimate Edition)
- 包管理工具: Apache Maven 3.9.6

对于 lab4，使用 C 实现，实验环境如下:

- 虚拟机工具: VMware Workstation Pro 17.5.0
- 虚拟机操作系统: Debian live 12.7.0 amd64 standard & Linux 6.1.0-25 amd64
- 宿主机操作系统: Windows 11 23H2
- 编程语言: C 23
- IDE: VSCode 1.95.1
- 编译调试工具: gcc (Debian 12.2.0-14) 12.2.0 与 GNU gdb (Debian 13.1-3) 13.1
- 网络管理工具: netplan.io 0.106-2+deb12u1
- 网络抓包工具: TShark (Wireshark) 4.0.11
- 防火墙管理工具: ufw 0.36.2
- 虚拟机 ssh 工具: OpenSSL 3.0.14
- 宿主机 ssh 工具: VSCode 1.95.1 & Termius 9.8.5
