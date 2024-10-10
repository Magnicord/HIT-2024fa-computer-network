package cn.edu.hit.utils;

import static cn.edu.hit.utils.StrUtils.hashString;

import java.io.*;

/**
 * 文件工具类，提供生成唯一文件名、读写对象的方法。
 */
public class FileUtils {

    /**
     * 生成唯一文件名。
     *
     * @param str 输入字符串
     * @param extension 文件扩展名
     * @return 生成的唯一文件名
     */
    public static String generateUniqueFileName(String str, String extension) {
        return hashString(str) + extension;
    }

    /**
     * 将对象写入文件。
     *
     * @param filePath 文件路径
     * @param obj 要写入的对象
     * @throws IOException 如果发生 I/O 错误
     */
    public static void writeObject(String filePath, Object obj) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(obj);
        }
    }

    /**
     * 从文件读取对象。
     *
     * @param filePath 文件路径
     * @param cls 对象的类类型
     * @param <T> 对象的类型
     * @return 读取的对象
     * @throws IOException 如果发生 I/O 错误
     * @throws ClassNotFoundException 如果类未找到
     * @throws ClassCastException 如果反序列化的对象不是指定类型的实例
     */
    public static <T> T readObject(String filePath, Class<T> cls) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Object obj = ois.readObject();
            if (cls.isInstance(obj)) {
                return cls.cast(obj);
            } else {
                throw new ClassCastException("Deserialized object is not an instance of " + cls.getName());
            }
        }
    }
}
