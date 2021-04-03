package main.java.g06.message.protocols;

import main.java.g06.Peer;
import main.java.g06.message.Message;
import main.java.g06.message.MessageType;
import main.java.g06.message.protocols.PutchunkProtocol;
import main.java.g06.message.protocols.StoredProtocol;

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
            new EnumMap.SimpleEntry<MessageType, ProtocolBuilderFI>(MessageType.REMOVED, RemovedProtocol::new)
    );

    public static Protocol build(Peer peer, Message message) {
        return constructors.get(message.type).constructor(peer, message);
    }
}
