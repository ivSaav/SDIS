package main.java.g06.message;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Message {
    private static final byte CR = 0xD, LF = 0xA, SEP = ' ';
    private static final byte[] CRLF = new byte[] {CR, LF};

    // <Version> <MessageType> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF>
    public String version;
    public MessageType type;
    public int senderId;
    public String fileId;
    public int chunkNo, replicationDegree;
    public byte[] body;


    public Message(String version, MessageType type, int senderId, String fileId, int chunkNo, int replicationDegree, byte[] body) {
        this.version = version;
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
        String version = new String(split.get(0));
//        int dotIndex = aux.indexOf('.');
//        int minorVersion = Integer.parseInt(aux.substring(0, dotIndex));
//        int majorVersion = Integer.parseInt(aux.substring(dotIndex + 1));

        MessageType type = MessageType.valueOf(new String(split.get(1)));

        int senderId = Integer.parseInt(new String(split.get(2)));
        String fileId = new String(split.get(3)).toLowerCase();

        if (type == MessageType.DELETE)
            return new Message(version, type, senderId, fileId, -1, -1, new byte[] {});

        int chunkNo = Integer.parseInt(new String(split.get(4)));
        if (type != MessageType.PUTCHUNK) {
            if (type == MessageType.CHUNK)
                return new Message(version, type, senderId, fileId, chunkNo, -1, split.get(5));
            else
                return new Message(version, type, senderId, fileId, chunkNo, -1, new byte[] {});
        }
        int replicationDegree = Integer.parseInt(new String(split.get(5)));
        return new Message(version, type, senderId, fileId, chunkNo, replicationDegree, split.get(6));
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
                    else {
                        split.add(new byte[] {});
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

    public static byte[] createMessage(String version, MessageType type, int senderId,
                               String fileId) {
        return createMessage(version, type, senderId, fileId, -1, -1, new byte[] {});
    }

    public static byte[] createMessage(String version, MessageType type, int senderId,
                               String fileId, int chunkNo) {
        return createMessage(version, type, senderId, fileId, chunkNo, -1, new byte[] {});
    }

    public static byte[] createMessage(String version, MessageType type, int senderId,
                               String fileId, int chunkNo, byte[] body) {
        return createMessage(version, type, senderId, fileId, chunkNo, -1, body);
    }

    public static byte[] createMessage(String version, MessageType type, int senderId,
                               String fileId, int chunkNo, int replicationDegree, byte[] body) {
        String header = version + " " + type + " " + senderId + " " + fileId + " "  + (chunkNo != -1 ? chunkNo + " " : "") + (replicationDegree != -1 ? replicationDegree + " " : "") + "\r\n\r\n";

        if (body.length == 0) {
            return header.getBytes(StandardCharsets.US_ASCII);
        }
        int messageSize = header.length() + body.length;
        byte[] result = new byte[messageSize];
        System.arraycopy(header.getBytes(StandardCharsets.US_ASCII), 0, result, 0, header.length());
        System.arraycopy(body, 0, result, header.length(), body.length);

        return result;
    }

    @Override
    public String toString() {
        return "Message{" +
                "version" + version +
                ", type=" + type +
                ", senderId=" + senderId +
                ", fileId='" + fileId + '\'' +
                ", chunkNo=" + chunkNo +
                ", replicationDegree=" + replicationDegree +
                ", body=" + body.length +
                '}';
    }
}
