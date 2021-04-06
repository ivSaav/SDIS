package main.g06.message.protocols;

import main.g06.Chunk;
import main.g06.Peer;
import main.g06.message.Message;
import main.g06.message.MessageType;

import java.util.Random;

public class RemovedProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public RemovedProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
        System.out.println(this.message);
       this.updateLocalChunkReplication(message.senderId, message.fileId, message.chunkNo);
       peer.setChangesFlag();
    }

    public void updateLocalChunkReplication(int senderId, String fileHash, int chunkNo) { // for REMOVED messages

        Chunk chunk = this.peer.getFileChunk(fileHash, chunkNo);
        int desiredReplication = this.peer.getFileReplication(fileHash);
        if (chunk != null) { // decremented the requested chunk's replication degree
            chunk.removeReplication(senderId); // remove perceived chunk replication

            if (chunk.getPerceivedReplication() < desiredReplication) { //replication bellow desired level

                System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA " + chunk.getPerceivedReplication());
                Random rand = new Random();
                int time = rand.nextInt(400);
                try {
                    Thread.sleep(time); // wait between 0 to 400 milliseconds
                }
                catch(InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                // TODO abort if it has received a PUTCHUNK message for the same chunk
                byte[] body = chunk.retrieve(peer.getId());
                byte[] message = Message.createMessage(this.peer.getVersion(), MessageType.PUTCHUNK,
                                                        this.peer.getId(), chunk.getFilehash(), chunk.getChunkNo(), desiredReplication, body);
                peer.getBackupChannel().multicast(message, message.length); // sending putchunk to other peers
            }
        }
    }
}
