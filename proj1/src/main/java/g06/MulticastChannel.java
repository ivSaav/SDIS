package main.java.g06;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastChannel {

    public static void multicast(byte[] message, int size, String addr, int port) {

        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(addr);

            DatagramPacket packet = new DatagramPacket(message, size, group, port);
            socket.send(packet);
            socket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
