package main.java.g06.message;

import main.java.g06.Peer;
import main.java.g06.message.protocols.Protocol;
import main.java.g06.message.protocols.ProtocolBuilder;

import java.net.DatagramPacket;

public class MessageThread extends Thread {

    protected final DatagramPacket packet;
    protected final Peer peer;
    protected Message message;
    protected Protocol protocol;

    public MessageThread(Peer peer, DatagramPacket packet) {
        this.peer = peer;
        this.packet = packet;
    }

    /**
     * Parses the packet it's responsible for
     * @return True if this packet does not belongs to the same peer
     */
    private boolean parsePacket() {
        this.message = Message.parse(packet.getData(), packet.getLength());
        return message.senderId != this.peer.getId();
    }

    @Override
    public void run() {
        if (parsePacket())
            return;

        // Choose and build message
        protocol = ProtocolBuilder.build(peer, message);
        protocol.start();
    }
}
