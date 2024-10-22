package cn.edu.hit.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class IOUtils {

    public static byte[] readBytesFromFile(String fileName) throws IOException {
        Path path = Path.of(fileName);
        return Files.readAllBytes(path);
    }

    public static void appendBytesToFile(String fileName, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName, true)) {
            fos.write(data);
            fos.flush();
        }
    }
}
