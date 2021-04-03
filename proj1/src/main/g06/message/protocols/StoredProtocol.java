package main.g06.message.protocols;

import main.g06.Peer;
import main.g06.message.Message;

public class StoredProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public StoredProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
//        Chunk chunk = peer.getChunk(message.fileId, message.chunkNo);
//        chunk.addPerceivedReplication();
//        System.out.printf("STORE from: %d  chunkNo: %d perceived: %d \n", message.senderId, message.chunkNo, chunk.getPerceivedRepDegree());
    }
}
