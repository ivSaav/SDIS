package main.java.g06.message.protocols;

import main.java.g06.Peer;
import main.java.g06.message.Message;

public class PutchunkProtocol implements main.java.g06.message.protocols.Protocol {
    Peer peer;

    public PutchunkProtocol(Peer peer, Message m) {
        this.peer = peer;
    }

    @Override
    public void start() {

    }
}
