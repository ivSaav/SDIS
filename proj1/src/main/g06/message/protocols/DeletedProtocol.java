package main.g06.message.protocols;

import main.g06.Peer;
import main.g06.SdisUtils;
import main.g06.message.Message;

public class DeletedProtocol implements Protocol {
    private final Peer peer;
    private final Message message;

    public DeletedProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
        if (!SdisUtils.isInitialVersion(peer.getVersion()))
            peer.removeUndeletedFile(message.senderId, message.fileId);
    }
}
