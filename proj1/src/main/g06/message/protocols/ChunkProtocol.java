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
        FileDetails fileDetails = peer.getFileDetails(message.fileId);
        ChunkMonitor cm;
        if (fileDetails == null || (cm = fileDetails.getMonitor(message.chunkNo)) == null)
            return;

        cm.markSolved(message.body, message.version);
    }
}
