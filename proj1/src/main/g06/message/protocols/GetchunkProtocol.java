package main.g06.message.protocols;

import main.g06.FileDetails;
import main.g06.Peer;
import main.g06.message.ChunkMonitor;
import main.g06.message.Message;
import main.g06.message.MessageType;

public class GetchunkProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public GetchunkProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
        System.out.printf("GETCHUNK from: %d   file: %s   chunkNo: %d\n", message.senderId, message.fileId.substring(0, 5), message.chunkNo);

        FileDetails fileDetails = peer.getFileDetails(message.fileId);
        if (fileDetails == null)
            return;

        ChunkMonitor cm = fileDetails.addMonitor(message.chunkNo);

        if (cm.await_send())
            return; // Another peer already sent the chunk

        byte[] body = fileDetails.getChunk(message.chunkNo).retrieve(peer.getId());
        byte[] messageBytes = Message.createMessage(peer.getVersion(), MessageType.CHUNK, peer.getId(), message.fileId, message.chunkNo, body);

        peer.getRestoreChannel().multicast(messageBytes, messageBytes.length);

        fileDetails.removeMonitor(message.chunkNo);
    }
}
