package main.java.g06.message.protocols;

import main.java.g06.Peer;
import main.java.g06.message.Message;

public class DeleteProtocol implements Protocol {
    Peer peer;

    public DeleteProtocol(Peer peer, Message m) {
        this.peer = peer;
    }

    @Override
    public void start() {

    }
}
