import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class MulticastDataReceiver extends Thread {

    private static int BUFFER_MAX_SIZE = 64500;

    private int peerId;
    private final String group;
    private final int port;
    private Message message;

    public MulticastDataReceiver(int peerId, String address, int port) {
        this.peerId = peerId;
        this.group = address;
        this.port = port;
        this.message = null;
    }

    @Override
    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(this.port);
            InetAddress address = InetAddress.getByName(this.group);
            socket.joinGroup(address);

            byte[] buffer = new byte[BUFFER_MAX_SIZE];
            
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Message m = Message.parse(packet.getData(), packet.getLength());

                if (m.senderId == this.peerId)
                    continue;

                this.message = m;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Message getMessage() {
        Message tmp = this.message;
        this.message = null;
        return tmp;
    }

}
