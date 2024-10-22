package cn.edu.hit.core;

import java.nio.ByteBuffer;
import java.util.Arrays;

import cn.edu.hit.config.Config;

public class Packet {

    private final int seqNum; // 序列号，存储为int类型，用于表示0-255的无符号byte
    private final byte[] data; // 数据部分，固定为1024字节
    private final int dataLength; // 数据的实际长度，使用int类型，占用2字节
    private final boolean eof; // EOF标志，使用boolean类型

    // 构造函数，初始化数据包，添加边界检查
    public Packet(int seqNum, byte[] data, int dataLength, boolean eof) {
        // 确保Seq在0-255的范围内
        if (seqNum < 0 || seqNum > 255) {
            throw new IllegalArgumentException("序列号Seq必须在0到255之间");
        }

        // 确保Data的长度固定为Config.DATA_SIZE
        if (data.length != Config.DATA_SIZE) {
            throw new IllegalArgumentException("数据部分Data的长度必须为" + Config.DATA_SIZE + "字节");
        }

        // 确保dataLength不超过数据的实际长度
        if (dataLength < 0 || dataLength > Config.DATA_SIZE) {
            throw new IllegalArgumentException("数据长度dataLength必须在0到" + Config.DATA_SIZE + "之间");
        }

        this.seqNum = seqNum;
        this.data = Arrays.copyOf(data, Config.DATA_SIZE); // 复制数据，而不是直接引用
        this.dataLength = dataLength; // 记录实际数据长度
        this.eof = eof; // 直接使用boolean类型表示EOF状态
    }

    // 从字节数组中恢复Packet对象，用于接收数据时的解析
    public static Packet fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int seqNum = Byte.toUnsignedInt(buffer.get()); // 将无符号byte转换为int
        byte[] data = new byte[Config.DATA_SIZE]; // 初始化固定大小的data数组
        buffer.get(data); // 将数据读取到data数组中
        int dataLength = Short.toUnsignedInt(buffer.getShort()); // 读取数据长度，使用2字节
        byte eofByte = buffer.get(); // 最后一个字节为EOF标志
        boolean eof = eofByte == 1; // 如果字节为1，则EOF为true，0为false

        return new Packet(seqNum, data, dataLength, eof); // 构造Packet对象并返回
    }

    // 获取序列号，返回int类型（0~255）
    public int getSeqNum() {
        return seqNum;
    }

    // 获取数据内容
    public byte[] getData() {
        return Arrays.copyOf(data, dataLength);
    }

    // 判断是否是最后一个数据包
    public boolean isEof() {
        return eof;
    }

    // 将Packet对象转换为字节数组，用于网络传输
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Config.BUFFER_SIZE); // 使用Config.BUFFER_SIZE
        buffer.put((byte)(seqNum & 0xFF)); // 将序列号转换为无符号byte
        buffer.put(data); // 将数据放入接下来的1024字节
        buffer.putShort((short)dataLength); // 将数据长度写入，使用2字节
        buffer.put((byte)(eof ? 1 : 0)); // 将EOF从boolean转换为byte
        return buffer.array(); // 返回字节数组
    }
}
