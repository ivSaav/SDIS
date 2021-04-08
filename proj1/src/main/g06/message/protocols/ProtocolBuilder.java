package main.g06.message.protocols;

import main.g06.Peer;
import main.g06.message.Message;
import main.g06.message.MessageType;

import java.util.EnumMap;
import java.util.Map;

public abstract class ProtocolBuilder {

    @FunctionalInterface
    private interface ProtocolBuilderFI {
        Protocol constructor(Peer p, Message m);
    }

    private static final Map<MessageType, ProtocolBuilderFI> constructors = Map.ofEntries(
            new EnumMap.SimpleEntry<MessageType, ProtocolBuilderFI>(MessageType.PUTCHUNK, PutchunkProtocol::new),
            new EnumMap.SimpleEntry<MessageType, ProtocolBuilderFI>(MessageType.GETCHUNK, GetchunkProtocol::new),
            new EnumMap.SimpleEntry<MessageType, ProtocolBuilderFI>(MessageType.CHUNK, ChunkProtocol::new),
            new EnumMap.SimpleEntry<MessageType, ProtocolBuilderFI>(MessageType.STORED, StoredProtocol::new),
            new EnumMap.SimpleEntry<MessageType, ProtocolBuilderFI>(MessageType.DELETE, DeleteProtocol::new),
            new EnumMap.SimpleEntry<MessageType, ProtocolBuilderFI>(MessageType.REMOVED, RemovedProtocol::new),
            new EnumMap.SimpleEntry<MessageType, ProtocolBuilderFI>(MessageType.INIT, InitProtocol::new),
            new EnumMap.SimpleEntry<MessageType, ProtocolBuilderFI>(MessageType.DELETED, DeletedProtocol::new)
    );

    public static Protocol build(Peer peer, Message message) {
        ProtocolBuilderFI pb = constructors.get(message.type);
        return pb == null ? null : pb.constructor(peer, message);
    }
}
