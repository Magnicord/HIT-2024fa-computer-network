package cn.edu.hit.core;

import java.nio.ByteBuffer;

import cn.edu.hit.config.CommonConfig;

/**
 * 确认包（ACK）类，包含序列号
 *
 * @param seqNum 序列号，存储为 int 类型（0~255 的无符号 byte）
 */
public record ACK(int seqNum) {

    /**
     * 构造方法，初始化 ACK 对象
     *
     * @param seqNum 序列号，必须在 0 到 255 之间
     * @throws IllegalArgumentException 如果序列号不在 0 到 255 之间
     */
    public ACK {
        if (seqNum < 0 || seqNum > 255) {
            throw new IllegalArgumentException("序列号 Seq 必须在 0 到 255 之间");
        }
    }

    /**
     * 从字节数组创建 ACK 对象
     *
     * @param bytes 字节数组
     * @return 创建的 ACK 对象
     * @throws IllegalArgumentException 如果字节数组长度不为 1
     */
    public static ACK fromBytes(byte[] bytes) {
        if (bytes.length != CommonConfig.ACK_SIZE) {
            throw new IllegalArgumentException("ACK 字节数组长度必须为 1");
        }
        int seqNum = Byte.toUnsignedInt(bytes[0]); // 将无符号 byte 转换为 int
        return new ACK(seqNum);
    }

    /**
     * 将 ACK 对象转换为字节数组
     *
     * @return 字节数组
     */
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(CommonConfig.ACK_SIZE); // 只需要 1 字节来存储序列号
        buffer.put((byte)(seqNum & 0xFF)); // 转换为无符号 byte
        return buffer.array();
    }
}
