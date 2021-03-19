import java.io.IOException;
import java.net.*;

public class MulticastPublisher {
    private DatagramSocket socket;
    private InetAddress group;

    private final String mcast_addr;
    private final int mcast_port;
    private final String server_addr;
    private final int svc_port;

    public MulticastPublisher(String mcast_addr, int mcast_port, String server_addr, int svc_port) {
        this.mcast_addr = mcast_addr;
        this.mcast_port = mcast_port;
        this.server_addr = server_addr;
        this.svc_port = svc_port;


        try {
            this.socket = new DatagramSocket(mcast_port);
            this.group = InetAddress.getByName(mcast_addr);
        }
        catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void multicast(String message) throws IOException {
        byte[] buf = message.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, group, mcast_port);
        socket.send(packet);
        String status = String.format("multicast: %s %d: %s %d", mcast_addr, mcast_port, server_addr, svc_port);
        System.out.println(status);
    }

    public void close() {
        this.socket.close();
    }
}