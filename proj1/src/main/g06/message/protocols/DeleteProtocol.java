package main.g06.message.protocols;

import main.g06.Chunk;
import main.g06.Peer;
import main.g06.SdisUtils;
import main.g06.message.Message;
import main.g06.message.MessageType;

import java.util.Collection;

public class DeleteProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public DeleteProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
        String fileHash = message.fileId;
        if (peer.getStoredFiles().containsKey(fileHash)){
            Collection<Chunk> chunks = peer.getStoredFiles().get(fileHash).getChunks();

            chunks.removeIf(chunk -> chunk.removeStorage(peer)); // remove chunk from fileHash List

            if (!(SdisUtils.isInitialVersion(peer.getVersion()) && SdisUtils.isInitialVersion(message.version))) {
                byte[] deletedMessage = Message.createMessage(peer.getVersion(), MessageType.DELETED, peer.getId(), message.fileId, message.chunkNo);
                peer.getControlChannel().multicast(deletedMessage);
            }

            if (chunks.isEmpty()) // remove file entry from files hashmap
                peer.removeStoredFile(fileHash);
            peer.setChangesFlag();
        }
    }
}
