package cn.edu.hit.core;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public interface Receiver {

    static Receiver createGBNReceiver(DatagramSocket socket, InetAddress serverAddress, int serverPort, int seqBits,
        double packetLossRate) {
        return new GBNReceiver(socket, serverAddress, serverPort, seqBits, packetLossRate);
    }

    static Receiver createSRReceiver(DatagramSocket socket, InetAddress serverAddress, int serverPort, int windowSize,
        int seqBits, double packetLossRate) {
        return new SRReceiver(socket, serverAddress, serverPort, windowSize, seqBits, packetLossRate);
    }

    void receiveData(String fileName) throws IOException;
}
