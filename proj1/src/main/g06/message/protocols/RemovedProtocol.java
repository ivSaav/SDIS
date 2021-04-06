package main.g06.message.protocols;

import main.g06.Chunk;
import main.g06.MulticastChannel;
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
       this.updateLocalChunkReplication(message.fileId, message.chunkNo);
    }

    public void updateLocalChunkReplication(String fileHash, int chunkNo) { // for REMOVED messages

        Chunk chunk = this.peer.getFileChunk(fileHash, chunkNo);
        if (chunk != null) { // decremented the requested chunk's replication degree
            chunk.removePerceivedRepDegree(); // decrement perceived chunk replication

            if (chunk.getPerceivedRepDegree() < chunk.getDesiredRepDegree()) { //replication bellow desired level

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
                                                        this.peer.getId(), chunk.getFilehash(), chunk.getChunkNo(), chunk.getDesiredRepDegree(), body);
                peer.getBackupChannel().multicast(message, message.length); // sending putchunk to other peers
            }
        }
    }
}
