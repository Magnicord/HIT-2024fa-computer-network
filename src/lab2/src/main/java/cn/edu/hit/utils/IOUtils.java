package cn.edu.hit.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class IOUtils {

    /**
     * 从文件中读取字节数据
     *
     * @param fileName 文件名
     * @return 文件的字节数据
     * @throws IOException 如果发生 I/O 错误
     */
    public static byte[] readBytesFromFile(String fileName) throws IOException {
        Path path = Path.of(fileName); // 获取文件路径
        return Files.readAllBytes(path); // 读取文件的所有字节数据
    }

    /**
     * 将字节数据追加到文件中
     *
     * @param fileName 文件名
     * @param data 要追加的数据
     * @throws IOException 如果发生 I/O 错误
     */
    public static void appendBytesToFile(String fileName, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName, true)) { // 创建文件输出流，追加模式
            fos.write(data); // 写入数据
            fos.flush(); // 刷新输出流
        }
    }
}
