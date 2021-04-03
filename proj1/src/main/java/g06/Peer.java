package main.java.g06;

import main.java.g06.message.Message;
import main.java.g06.message.MessageType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class Peer implements ClientPeerProtocol {

    private final int id;
    private final String version;
    private final Map<String, FileDetails> fileHashes; // filename --> FileDetail
    private final Set<String> storedChunks;

    private final String mcAddr;
    private final int mcPort;
    private final String mdbAddr;
    private final int mdbPort;
    private final String mdrAddr;
    private final int mdrPort;
    private final MulticastReceiver dataReceiver;
    private final MulticastReceiver controlReceiver;

    public Peer(String version, int id, String MC, String MDB, String MDR) {
        this.id = id;
        this.version = version;
        // TODO: Reload this data from disk
        this.fileHashes = new HashMap<>();
        this.storedChunks = new HashSet<>();

        String[] vals = MC.split(":"); //MC

        this.mcAddr = vals[0];
        this.mcPort = Integer.parseInt(vals[1]);

        vals = MDB.split(":");
        this.mdbAddr = vals[0];
        this.mdbPort = Integer.parseInt(vals[1]);

        vals = MDR.split(":");
        this.mdrAddr = vals[0];
        this.mdrPort = Integer.parseInt(vals[1]);

        this.dataReceiver = new MulticastReceiver(this.id, mdbAddr, mdbPort);
        this.controlReceiver = new MulticastReceiver(this.id, this.mcAddr, this.mcPort);
    }

    @Override
    public String backup(String path, int repDegree) {

        String fileHash = SdisUtils.createFileHash(path);
        if (fileHash == null)
            return "failure";

        try {
            File file = new File(path);
            int num_chunks = (int) Math.floor( (double) file.length() / (double) Definitions.CHUNK_SIZE) + 1;

            // Save filename and its generated hash
            // TODO: Write to file in case peer crashes we must resume this operation
            this.fileHashes.put(path, new FileDetails(fileHash, file.length(), repDegree));

            FileInputStream fstream = new FileInputStream(file);
            byte[] chunk_data = new byte[Definitions.CHUNK_SIZE];
            int num_read, last_num_read = -1, chunkNo = 0;

            // Read file chunks and send them
            while ((num_read = fstream.read(chunk_data)) != -1) {
                // Send message
                byte[] message = Message.createMessage(this.version, MessageType.PUTCHUNK, this.id, fileHash, chunkNo, repDegree, chunk_data);
                MulticastChannel.multicast(message, message.length, this.mdbAddr, this.mdbPort);
                System.out.printf("MDB: chunkNo %d ; size %d\n", chunkNo, num_read);

                // TODO: Repeat message N times if rep degree was not reached
                // TODO: Advance chunk only after rep degree was reached

                last_num_read = num_read;
                chunkNo++;
            }

            if (last_num_read == Definitions.CHUNK_SIZE) {
                byte[] message = Message.createMessage(this.version, MessageType.PUTCHUNK, this.id, fileHash, chunkNo, repDegree, new byte[] {});
                MulticastChannel.multicast(message, message.length, this.mdbAddr, this.mdbPort);
                System.out.printf("MDB: chunkNo %d ; size %d\n", chunkNo, num_read);
            }

            fstream.close();
            System.out.println("Created " + num_chunks + " chunks");
        }
        catch (IOException e) {
            e.printStackTrace();
            return "failure";
        }

        return "success";
    }

    @Override
    public String delete(String file) throws RemoteException {
        return null;
    }

    //    @Override
//    public String delete(String file) {
//        if (this.fileHashes.containsKey(file)) {
//            String hash = fileHashes.get(file);
//            byte[] message = Message.createMessage(this.version, MessageType.DELETE, this.id, hash);
//            MulticastChannel.multicast(message, message.length, this.mcAddr, this.mcPort);
//            System.out.printf("MC: DELETE %s\n", file);
//            // TODO remove file from fileHashes
//            // not removing yet because peer doesn't always get the delete file message
//        }
//        return "success";
//    }
//
//    public void sendStorageResponse(String fileId, int chunkNo) {
//        byte[] message = Message.createMessage(this.version, MessageType.STORED, this.id, fileId, chunkNo);
//        Random rand = new Random();
//        int time = rand.nextInt(400);
//        try {
//            Thread.sleep(time);
//        }
//        catch(InterruptedException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//        MulticastChannel.multicast(message, message.length, this.mcAddr, this.mcPort);
//    }
//
//    public static void main(String[] args) throws IOException{
//
//        if (args.length < 1) {
//            System.out.println("usage: Peer <remote_object_name>");
//            throw new IOException("Invalid usage");
//        }
//
//        String version = args[0];
//        int id = Integer.parseInt(args[1]);
//        String service_ap = args[2];
//
//        String MC = args[3], MDB = args[4], MDR = args[5];
//
//        Peer peer = new Peer(version, id, MC, MDB, MDR);
//        Registry registry = LocateRegistry.getRegistry();
//        try {
//            ClientPeerProtocol stub = (ClientPeerProtocol) UnicastRemoteObject.exportObject(peer,0);
//
//            //Bind the remote object's stub in the registry
//            registry.bind(service_ap, stub); //register peer object with the name in args[0]
//        } catch (AlreadyBoundException e) {
//            System.out.println("Object already bound! Rebinding...");
//            registry.rebind(service_ap, peer);
//        }
//        System.out.println("Peer " + peer.id + " ready");
//
//        peer.dataReceiver.start();
//        peer.controlReceiver.start();
//        while (true) {
//            Message message = null;
//
//            if ((message = peer.dataReceiver.getMessage()) != null) { //chunk backup
//                Chunk chunk = new Chunk(message.fileId, message.chunkNo, message.body.length, message.replicationDegree, message.body);
//                System.out.println(message.toString());
//                peer.addFileEntry(null, message.fileId);
//                peer.addChunk(message.fileId, chunk);
//                chunk.store(peer.id);
//                peer.sendStorageResponse(message.fileId, message.chunkNo);
//            }
//
//            if ((message = peer.controlReceiver.getMessage()) != null) {
//                System.out.println(message);
//
//                if (message.type == MessageType.STORED) { //stored message
//                    Chunk chunk = peer.getChunk(message.fileId, message.chunkNo);
//                    chunk.addPerceivedReplication();
//                    System.out.printf("STORE from: %d  chunkNo: %d perceived: %d \n", message.senderId, message.chunkNo, chunk.getPerceivedRepDegree());
//                }
//                else if (message.type == MessageType.DELETE) { //delete message
//                    peer.removeFileFromStorage(message.fileId);
//                }
//            }
//        }
//    }
//
//    private void removeFileFromStorage(String fileHash){
//        if (this.files.containsKey(fileHash)){
//            List<Chunk> chunks = files.get(fileHash);
//            chunks.removeIf(chunk -> chunk.removeStorage(this.id)); // remove chunk from fileHash List
//
//            if (chunks.isEmpty()) // remove file entry from files hashmap
//                this.files.remove(fileHash);
//        }
//    }


    public int getId() {
        return id;
    }
}
