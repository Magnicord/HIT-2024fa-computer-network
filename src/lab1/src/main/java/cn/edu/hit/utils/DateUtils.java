package cn.edu.hit.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类，提供解析和格式化 HTTP 日期的方法。
 */
public class DateUtils {

    // 日期格式 RFC 1123
    private static final DateTimeFormatter httpDateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;

    /**
     * 解析 HTTP 日期时间字符串为 ZonedDateTime 对象。
     *
     * @param date HTTP 日期时间字符串
     * @return 解析后的 ZonedDateTime 对象
     */
    public static ZonedDateTime parseHttpDateTime(String date) {
        return ZonedDateTime.parse(date, httpDateFormatter);
    }

    /**
     * 将 ZonedDateTime 对象格式化为 HTTP 日期时间字符串。
     *
     * @param date ZonedDateTime 对象
     * @return 格式化后的 HTTP 日期时间字符串
     */
    public static String parseHttpDateTime(ZonedDateTime date) {
        return httpDateFormatter.format(date);
    }
}
