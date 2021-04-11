package main.g06.message.protocols;

import main.g06.Chunk;
import main.g06.Peer;
import main.g06.SdisUtils;
import main.g06.message.Message;
import main.g06.message.MessageType;

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
        Chunk chunk = new Chunk(message.fileId, message.chunkNo, message.body.length);

        if (peer.isInitiator(message.fileId)) // for PUTCHUNK messages sent because of the REMOVED PROTOCOL (ignore chunk)
            return;

        if (!peer.hasDiskSpace(chunk.getSize())) { //checking if there is enough disk space for this chunk
            System.out.println("Not enough space for chunk " + chunk.getChunkNo());
            return;
        }

        if (peer.hasStoredChunk(chunk)) {// already have a local copy
            //mark as solved if the chunk has a monitor associated
            // a monitor is added when Receiving a REMOVED message and the desired replication degree isn't fulfilled
            peer.resolveRemovedChunk(message.fileId, message.chunkNo); // prevent PUTCHUNK message
        } else {
            peer.addStoredChunk(chunk, message.replicationDegree);
            chunk.store(peer, message.body);
        }

        this.confirmStorage(chunk);
    }

    /**
     * Sends a confirmation message (STORED) to other peers
     * v2.0 cancels message storage when the desired replication has already been reached
     * @param chunk - chunk to be confirmed
     */
    public void confirmStorage(Chunk chunk) {
        byte[] message = Message.createMessage(peer.getVersion(), MessageType.STORED, peer.getId(), chunk.getFilehash(), chunk.getChunkNo());

        Random rand = new Random();
        int time = rand.nextInt(400);
        try {
            Thread.sleep(time);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Enhancement for v2.0
        // aborting storage operation
        if (!SdisUtils.isInitialVersion(peer.getVersion()) && chunk.getPerceivedReplication() > peer.getFileReplication(chunk.getFilehash())) {
            this.undoStorage(chunk); //remove local storage
        }
        else {
            peer.getControlChannel().multicast(message);
            peer.setChangesFlag();
        }
    }

    /**
     * Removes unnecessary replications (v2.0)
     * Informs other peers of this operation
     * @param chunk - chunk to be removed from storage
     */
    private void undoStorage(Chunk chunk) {
        if (!chunk.removeStorage(peer))
            return; // couldn't remove file

        this.peer.removeStoredChunk(chunk);
    }
}
