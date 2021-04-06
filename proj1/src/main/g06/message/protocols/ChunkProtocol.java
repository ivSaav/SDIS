package main.g06.message.protocols;

import main.g06.FileDetails;
import main.g06.Peer;
import main.g06.message.ChunkMonitor;
import main.g06.message.Message;

public class ChunkProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public ChunkProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
        System.out.printf("CHUNK from: %d   file: %s   chunkNo: %d\n", message.senderId, message.fileId.substring(0, 5), message.chunkNo);

        FileDetails fileDetails = peer.getFileDetails(message.fileId);
        ChunkMonitor cm;
        if (fileDetails == null || (cm = fileDetails.getMonitor(message.chunkNo)) == null)
            return;

        cm.markSolved(message.body);
    }
}
