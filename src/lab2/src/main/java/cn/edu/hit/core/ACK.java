package cn.edu.hit.core;

import java.nio.ByteBuffer;

import cn.edu.hit.config.Config;

public class ACK {

    private final int seqNum; // 序列号，存储为int类型（0~255的无符号byte）

    // 构造函数，传入序列号
    public ACK(int seqNum) {
        if (seqNum < 0 || seqNum > 255) {
            throw new IllegalArgumentException("序列号Seq必须在0到255之间");
        }
        this.seqNum = seqNum;
    }

    // 从字节数组恢复ACK对象
    public static ACK fromBytes(byte[] bytes) {
        if (bytes.length != Config.ACK_SIZE) {
            throw new IllegalArgumentException("ACK字节数组长度必须为1");
        }
        int seqNum = Byte.toUnsignedInt(bytes[0]); // 将无符号byte转换为int
        return new ACK(seqNum);
    }

    // 获取序列号
    public int getSeqNum() {
        return seqNum;
    }

    // 将ACK对象转换为字节数组，用于网络传输
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Config.ACK_SIZE); // 只需要1字节来存储序列号
        buffer.put((byte)(seqNum & 0xFF)); // 转换为无符号byte
        return buffer.array();
    }
}
