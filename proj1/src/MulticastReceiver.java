import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class MulticastReceiver {

    private MulticastSocket socket;
    private InetAddress group;
    private byte[] buf = new byte[256];

    private String mcast_addr;
    private int mcast_port;

    public MulticastReceiver(String mcast_adr, int mcast_port) {
        this.mcast_addr = mcast_adr;
        this.mcast_port = mcast_port;

        try {
            socket = new MulticastSocket(mcast_port);
            group = InetAddress.getByName(mcast_adr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receive() {
        try {
            socket.joinGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive()
        }

    }


}