package main.g06.message.protocols;

import main.g06.Chunk;
import main.g06.Peer;
import main.g06.message.Message;
import main.g06.message.MessageType;

import java.util.Arrays;
import java.util.Random;

public class PutchunkProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public PutchunkProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
        System.out.println(message.toString());
        Chunk chunk = new Chunk(message.fileId, message.chunkNo, message.body.length);

        if (peer.isInitiator(message.fileId)) // for PUTCHUNK messages sent because of the REMOVED PROTOCOL (ignore chunk)
            return;

        if (!peer.hasDiskSpace(chunk.getSize())) { //checking if there is enough disk space for this chunk
            System.out.println("Not enough space for chunk " + chunk.getChunkNo());
            return;
        }
        if (peer.hasStoredChunk(chunk)) {// already have a local copy (do nothing)
            //mark as solved if the chunk has a monitor associated
            // a monitor is added when Receiving a REMOVED message and the desired replication degree isn't fulfilled
            peer.resolveRemovedChunk(message.fileId, message.chunkNo); // prevent PUTCHUNK message
            return;
        }

        peer.addStoredChunk(chunk, message.replicationDegree);
        chunk.store(peer.getId(), message.body);
        this.sendStorageResponse(message.fileId, message.chunkNo);
        peer.setChangesFlag();
    }

    public void sendStorageResponse(String fileId, int chunkNo) {
        byte[] message = Message.createMessage(peer.getVersion(), MessageType.STORED, peer.getId(), fileId, chunkNo);
        Random rand = new Random();
        int time = rand.nextInt(400);
        try {
            Thread.sleep(time);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
        peer.getControlChannel().multicast(message, message.length);
    }
}
