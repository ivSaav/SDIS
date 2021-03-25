import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastDataChannel {

    public static void multicast(byte[] message, String addr, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(addr);

            DatagramPacket packet = new DatagramPacket(message, message.length, group, port);
            socket.send(packet);
            socket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
