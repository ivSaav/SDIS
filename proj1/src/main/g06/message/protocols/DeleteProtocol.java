package main.g06.message.protocols;

import main.g06.Chunk;
import main.g06.Peer;
import main.g06.message.Message;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DeleteProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public DeleteProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
        System.out.printf("DELETE from: %d ; fileID: %s\n", message.senderId, message.fileId);
        this.removeFileFromStorage(message.fileId);
    }

    private void removeFileFromStorage(String fileHash){
        if (peer.getStoredFiles().containsKey(fileHash)){
            Collection<Chunk> chunks = peer.getStoredFiles().get(fileHash).getChunks();

            chunks.removeIf(chunk -> chunk.removeStorage(peer.getId())); // remove chunk from fileHash List

            if (chunks.isEmpty()) // remove file entry from files hashmap
                peer.removeStoredFile(fileHash);
            peer.setChangesFlag();
        }
    }
}
