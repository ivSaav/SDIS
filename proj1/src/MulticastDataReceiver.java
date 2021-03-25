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
    private String message;

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

                this.message = new String(packet.getData());
                //TODO convert to Message
                //TODO Ignore messages from the creating peer
                Thread.sleep(400);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public String getMessage() {
        String tmp = this.message;
        this.message = null;
        return tmp;
    }

}
