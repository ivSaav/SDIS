package main.java.g06.message.protocols;

import main.java.g06.Peer;
import main.java.g06.message.Message;

public class RemovedProtocol implements Protocol {
    Peer peer;

    public RemovedProtocol(Peer peer, Message m) {
        this.peer = peer;
    }

    @Override
    public void start() {

    }
}
