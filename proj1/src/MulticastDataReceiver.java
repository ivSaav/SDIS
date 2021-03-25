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

                byte[] body = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, body, 0, packet.getLength()); // resize array
                this.message = new String(body);
                System.out.println("Length " + packet.getLength());
                //TODO convert to Message
                //TODO Ignore messages from the creating peer
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getMessage() {
        String tmp = this.message;
        this.message = null;
        return tmp;
    }

}
