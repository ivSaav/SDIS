package main.g06.message.protocols;

import main.g06.Chunk;
import main.g06.FileDetails;
import main.g06.Peer;
import main.g06.SdisUtils;
import main.g06.message.ChunkMonitor;
import main.g06.message.Message;
import main.g06.message.MessageType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class GetchunkProtocol implements Protocol {

    private final Peer peer;
    private final Message message;

    public GetchunkProtocol(Peer peer, Message message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void start() {
        FileDetails fileDetails = peer.getFileDetails(message.fileId);
        if (fileDetails == null)
            return;

        Chunk chunk = fileDetails.getChunk(message.chunkNo);
        if (chunk == null)
            return;

        ChunkMonitor cm = fileDetails.addMonitor(message.chunkNo);

        if (cm.await_send())
            return; // Another peer already sent the chunk

        fileDetails.removeMonitor(message.chunkNo);

        if (SdisUtils.isInitialVersion(message.version)) {
            // Initial version
            byte[] body = chunk.retrieve(peer);
            byte[] messageBytes = Message.createMessage(message.version, MessageType.CHUNK, peer.getId(), message.fileId, message.chunkNo, body);
            peer.getRestoreChannel().multicast(messageBytes);
        } else {
            // Improvement
            try {
                ServerSocket serverSocket = new ServerSocket(0);
                serverSocket.setSoTimeout(2000);

                byte[] body = (serverSocket.getInetAddress().getHostName() + ":" + serverSocket.getLocalPort()).getBytes(StandardCharsets.US_ASCII);
                byte[] messageBytes = Message.createMessage(peer.getVersion(), MessageType.CHUNK, peer.getId(), message.fileId, message.chunkNo, body);
                peer.getRestoreChannel().multicast(messageBytes);

                Socket socket = serverSocket.accept();

                OutputStream os = socket.getOutputStream();
                os.write(chunk.retrieve(peer));

                socket.shutdownOutput();
                socket.close();

                serverSocket.close();
            } catch (SocketTimeoutException e) {
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
