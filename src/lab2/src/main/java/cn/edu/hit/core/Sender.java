package cn.edu.hit.core;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public interface Sender {

    static Sender createGBNSender(DatagramSocket socket, InetAddress clientAddress, int clientPort, int windowSize,
        int seqBits, double packetLossRate, long timeout) throws SocketException {
        return new GBNSender(socket, clientAddress, clientPort, windowSize, seqBits, packetLossRate, timeout);
    }

    static Sender createSRSender(DatagramSocket socket, InetAddress clientAddress, int clientPort, int windowSize,
        int seqBits, double packetLossRate, long timeout) throws SocketException {
        return new SRSender(socket, clientAddress, clientPort, windowSize, seqBits, packetLossRate, timeout);
    }

    void sendFile(String fileName) throws IOException;

    void sendData(byte[] data) throws IOException;
}
