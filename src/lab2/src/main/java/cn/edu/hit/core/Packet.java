package cn.edu.hit.core;

import java.nio.ByteBuffer;
import java.util.Arrays;

import cn.edu.hit.config.CommonConfig;
import lombok.Getter;

public class Packet {

    @Getter
    // 序列号，存储为 int 类型，用于表示 0-255 的无符号 byte
    private final int seqNum;
    // 数据部分，固定为 1024 字节
    private final byte[] data;
    // 数据的实际长度，使用 int 类型，占用 2 字节
    private final int dataLength;
    @Getter
    private final boolean eof; // EOF 标志，使用 boolean 类型

    /**
     * 构造方法，初始化 Packet 对象
     *
     * @param seqNum 序列号，必须在 0 到 255 之间
     * @param data 数据部分，长度必须为 CommonConfig.DATA_SIZE
     * @param dataLength 数据的实际长度，必须在 0 到 CommonConfig.DATA_SIZE 之间
     * @param eof EOF 标志，表示是否为文件末尾
     * @throws IllegalArgumentException 如果参数不符合要求
     */
    public Packet(int seqNum, byte[] data, int dataLength, boolean eof) {
        // 确保 Seq 在 0-255 的范围内
        if (seqNum < 0 || seqNum > 255) {
            throw new IllegalArgumentException("序列号 Seq 必须在 0 到 255 之间");
        }

        // 确保 Data 的长度固定为 Config.DATA_SIZE
        if (data.length != CommonConfig.DATA_SIZE) {
            throw new IllegalArgumentException("数据部分 Data 的长度必须为 " + CommonConfig.DATA_SIZE + " 字节");
        }

        // 确保 dataLength 不超过数据的实际长度
        if (dataLength < 0 || dataLength > CommonConfig.DATA_SIZE) {
            throw new IllegalArgumentException("数据长度 dataLength 必须在 0 到 " + CommonConfig.DATA_SIZE + " 之间");
        }

        this.seqNum = seqNum;
        this.data = Arrays.copyOf(data, CommonConfig.DATA_SIZE); // 复制数据，而不是直接引用
        this.dataLength = dataLength; // 记录实际数据长度
        this.eof = eof; // 直接使用 boolean 类型表示 EOF 状态
    }

    /**
     * 从字节数组创建 Packet 对象
     *
     * @param bytes 字节数组
     * @return 创建的 Packet 对象
     */
    public static Packet fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes); // 包装字节数组为 ByteBuffer
        int seqNum = Byte.toUnsignedInt(buffer.get()); // 将无符号 byte 转换为 int
        byte[] data = new byte[CommonConfig.DATA_SIZE]; // 初始化固定大小的 data 数组
        buffer.get(data); // 将数据读取到 data 数组中
        int dataLength = Short.toUnsignedInt(buffer.getShort()); // 读取数据长度，使用 2 字节
        byte eofByte = buffer.get(); // 最后一个字节为 EOF 标志
        boolean eof = eofByte == 1; // 如果字节为 1，则 EOF 为 true，0 为 false

        return new Packet(seqNum, data, dataLength, eof); // 构造 Packet 对象并返回
    }

    /**
     * 获取数据部分
     *
     * @return 数据部分的字节数组
     */
    public byte[] getData() {
        return Arrays.copyOf(data, dataLength); // 复制实际数据长度的字节数组
    }

    /**
     * 将 Packet 对象转换为字节数组
     *
     * @return 字节数组
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(CommonConfig.BUFFER_SIZE); // 使用 Config.BUFFER_SIZE
        buffer.put((byte)(seqNum & 0xFF)); // 将序列号转换为无符号 byte
        buffer.put(data); // 将数据放入接下来的 1024 字节
        buffer.putShort((short)dataLength); // 将数据长度写入，使用 2 字节
        buffer.put((byte)(eof ? 1 : 0)); // 将 EOF 从 boolean 转换为 byte
        return buffer.array(); // 返回字节数组
    }
}
