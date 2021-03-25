import enums.MessageType;

import javax.swing.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Message {
    private static final byte CR = 0xD, LF = 0xA, SEP = ' ';
    private static final byte[] CRLF = new byte[] {CR, LF};

    // <Version> <MessageType> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF>
    public int majorVersion, minorVersion;
    public MessageType type;
    public int senderId;
    public String fileId;
    public int chunkNo, replicationDegree;
    public byte[] body;


    public Message(int majorVersion, int minorVersion, MessageType type, int senderId, String fileId, int chunkNo, int replicationDegree, byte[] body) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.type = type;
        this.senderId = senderId;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.replicationDegree = replicationDegree;
        this.body = body;
    }

    public static Message parse(byte[] data, int size) {
        List<byte[]> split = Message.split(data, size);

        // Parse version
        String aux = new String(split.get(0));
        int dotIndex = aux.indexOf('.');
        int minorVersion = Integer.parseInt(aux.substring(0, dotIndex));
        int majorVersion = Integer.parseInt(aux.substring(dotIndex + 1));

        MessageType type = MessageType.valueOf(new String(split.get(1)));

        int senderId = Integer.parseInt(new String(split.get(2)));
        String fileId = new String(split.get(3)).toLowerCase();

        if (type == MessageType.DELETE)
            return new Message(minorVersion, majorVersion, type, senderId, fileId, 0, 0, null);

        int chunkNo = Integer.parseInt(new String(split.get(4)));
        if (type != MessageType.PUTCHUNK) {
            if (type == MessageType.CHUNK)
                return new Message(minorVersion, majorVersion, type, senderId, fileId, chunkNo, 0, split.get(5));
            else
                return new Message(minorVersion, majorVersion, type, senderId, fileId, chunkNo, 0, null);
        }

        int replicationDegreee = Integer.parseInt(new String(split.get(5)));
        return new Message(minorVersion, majorVersion, type, senderId, fileId, chunkNo, replicationDegreee, split.get(6));
    }


    public static List<byte[]> split(byte[] data, int size) {
        List<byte[]> split = new ArrayList<>();

        boolean found_arg = false;

        int start = 0;
        for (int i = 0; i < data.length; i++) {
            if (!found_arg) {
                start = i;
                if (data[i] != Message.SEP) {
                    found_arg = true;
                }
            } else {
                // May contain CRLF*2
                if (i - start == 4 && (data[start] == Message.CR && data[start + 1] == Message.LF && data[start + 2] == Message.CR && data[start + 3] == Message.LF)) {
                    if (i != size - 1) {
                        // Has body
                        split.add(Arrays.copyOfRange(data, i, size));
                    }

                    return split;
                }

                if (data[i] == Message.SEP) {
                    split.add(Arrays.copyOfRange(data, start, i));
                    found_arg = false;
                }
            }
        }

        // Should NOT get here
        return split;
    }


    @Override
    public String toString() {
        return "Message{" +
                "majorVersion=" + majorVersion +
                ", minorVersion=" + minorVersion +
                ", type=" + type +
                ", senderId=" + senderId +
                ", fileId='" + fileId + '\'' +
                ", chunkNo=" + chunkNo +
                ", replicationDegree=" + replicationDegree +
                ", body=" + (body == null ? null : body.length) +
                '}';
    }
}
