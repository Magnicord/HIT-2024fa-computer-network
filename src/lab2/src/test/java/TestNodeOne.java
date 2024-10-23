import cn.edu.hit.app.App;
import cn.edu.hit.config.AppConfig;

public class TestNodeOne {

    public static void main(String[] args) throws Exception {
        String dstIp = AppConfig.DEFAULT_IP;
        int srcPort = 20001;
        int dstPort = 20002;
        App app = new App(dstIp, dstPort, srcPort);
        app.start();
    }
}
