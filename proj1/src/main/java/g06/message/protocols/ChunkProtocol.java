package main.java.g06.message.protocols;

import main.java.g06.Peer;
import main.java.g06.message.Message;

public class ChunkProtocol implements Protocol {
    Peer peer;

    public ChunkProtocol(Peer peer, Message m) {
        this.peer = peer;
    }

    @Override
    public void start() {

    }
}
