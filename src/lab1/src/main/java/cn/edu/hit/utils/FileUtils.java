package cn.edu.hit.utils;

import static cn.edu.hit.utils.StrUtils.hashString;

import java.io.*;

public class FileUtils {

    public static String generateUniqueFileName(String str, String extension) {
        return hashString(str) + extension;
    }

    public static void writeObject(String filePath, Object obj) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(obj);
        }
    }

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
