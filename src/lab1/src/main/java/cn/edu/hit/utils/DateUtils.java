package cn.edu.hit.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private static final DateTimeFormatter httpDateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME; // 日期格式 RFC 1123

    public static ZonedDateTime parseHttpDateTime(String date) {
        return ZonedDateTime.parse(date, httpDateFormatter);
    }

    public static String parseHttpDateTime(ZonedDateTime date) {
        return httpDateFormatter.format(date);
    }
}
