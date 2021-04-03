package main.g06.message.protocols;

import main.g06.Peer;
import main.g06.message.Message;

public class RemovedProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public RemovedProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {

    }
}
