import cn.edu.hit.app.App;
import cn.edu.hit.config.AppConfig;

public class TestNodeTwo {

    public static void main(String[] args) throws Exception {
        String dstIp = AppConfig.DEFAULT_IP;
        int srcPort = 20002;
        int dstPort = 20001;
        App app = new App(dstIp, dstPort, srcPort);
        app.start();
    }
}
