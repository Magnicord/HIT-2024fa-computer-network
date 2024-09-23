package cn.edu.hit.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private static final DateTimeFormatter httpDateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME; // 日期格式 RFC 1123

    public static LocalDateTime parseHttpDateTime(String date) {
        return LocalDateTime.parse(date, httpDateFormatter);
    }

    public static String parseHttpDateTime(LocalDateTime date) {
        return httpDateFormatter.format(date);
    }
}
