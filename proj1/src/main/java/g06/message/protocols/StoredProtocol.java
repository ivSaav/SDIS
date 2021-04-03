package main.java.g06.message.protocols;

import main.java.g06.Peer;
import main.java.g06.message.Message;

public class StoredProtocol implements main.java.g06.message.protocols.Protocol {
    Peer peer;

    public StoredProtocol(Peer peer, Message message) {
        this.peer = peer;
    }

    @Override
    public void start() {

    }
}
