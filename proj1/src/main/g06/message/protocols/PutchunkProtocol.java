package main.g06.message.protocols;

import main.g06.Chunk;
import main.g06.Peer;
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
        Chunk chunk = new Chunk(message.fileId, message.chunkNo, message.body.length, message.replicationDegree);
        System.out.println(message.toString());
        chunk.store(peer.getId(), message.body);
        peer.addStoredChunk(chunk);
        this.sendStorageResponse(message.fileId, message.chunkNo);
        peer.backupState();
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
