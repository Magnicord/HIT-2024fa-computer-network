package cn.edu.hit.config;

public class AppConfig {

    public static final String TIME_PROMPT = "-time";
    public static final String TIME_PROMPT_HELP = "向服务器请求获取当前时间";
    public static final String QUIT_PROMPT = "-quit";
    public static final String QUIT_PROMPT_HELP = "退出客户端下载模式";
    public static final String GBN_PROMPT_PREFIX = "-testgbn";
    public static final String GBN_PROMPT = "-testgbn [fileName]";
    public static final String GBN_PROMPT_HELP = "向服务器请求测试GBN协议，fileName为要下载的文件名";
    public static final String SR_PROMPT_PREFIX = "-testsr";
    public static final String SR_PROMPT = "-testsr [fileName]";
    public static final String SR_PROMPT_HELP = "向服务器请求测试SR协议，fileName为要下载的文件名";
    public static final String QUIT_REPLY_MSG = "Goodbye!";
    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_IP = "127.0.0.1";
}
