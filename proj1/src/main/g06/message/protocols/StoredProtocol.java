package main.g06.message.protocols;

import main.g06.Chunk;
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
        System.out.printf("STORED from: %d  chunkNo: %d \n", message.senderId, message.chunkNo);
        peer.addPerceivedReplication(message.senderId, message.fileId, message.chunkNo);
        // marks a chunk as solved if the desired replication degree has been reached
        //only for initiator peer
        peer.resolveChunk(message.fileId, message.chunkNo);
        peer.setChangesFlag();
    }
}
