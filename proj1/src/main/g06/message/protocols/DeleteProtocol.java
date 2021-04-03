package main.g06.message.protocols;

import main.g06.Peer;
import main.g06.message.Message;

public class DeleteProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public DeleteProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
//        peer.removeFileFromStorage(message.fileId);
    }

//    private void removeFileFromStorage(String fileHash){
//        if (this.files.containsKey(fileHash)){
//            List<Chunk> chunks = files.get(fileHash);
//            chunks.removeIf(chunk -> chunk.removeStorage(this.id)); // remove chunk from fileHash List
//
//            if (chunks.isEmpty()) // remove file entry from files hashmap
//                this.files.remove(fileHash);
//        }
//    }
}
