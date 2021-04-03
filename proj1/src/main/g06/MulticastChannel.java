package main.g06;

import main.g06.message.MessageThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.ThreadPoolExecutor;

public class MulticastChannel extends Thread {

    private static final int BUFFER_MAX_SIZE = 64500;

    private final Peer peer;
    private final String group;
    private final int port;
    private final ThreadPoolExecutor poolExecutor;

    public MulticastChannel(Peer peer, String address, int port, ThreadPoolExecutor poolExecutor) {
        this.peer = peer;
        this.group = address;
        this.port = port;
        this.poolExecutor = poolExecutor;
    }

    @Override
    public void run() {

        try {
            MulticastSocket socket = new MulticastSocket(this.port);
            InetAddress address = InetAddress.getByName(this.group);
            socket.joinGroup(address);

            byte[] buffer = new byte[BUFFER_MAX_SIZE];
            
            while (true) {
                if (this.isInterrupted()) {
                    poolExecutor.shutdown();
                    return;
                }

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                MessageThread mt = new MessageThread(peer, packet);
                this.poolExecutor.submit(mt);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void multicast(byte[] message, int size) {

        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(this.group);

            DatagramPacket packet = new DatagramPacket(message, size, address, this.port);
            socket.send(packet);
            socket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
