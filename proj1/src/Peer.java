import enums.MessageType;

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
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class Peer implements ClientPeerProtocol {

    private final int id;
    private final String version;
    private Map<String, List<Chunk>> files;
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
        this.files = new HashMap<String, List<Chunk>>();

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

        String fileHash = this.createFileHash(path);
        List<Chunk> fileChunks = this.createChunks(path, fileHash, repDegree);

        for (Chunk chunk : fileChunks) {
            byte[] message = Message.createMessage(this.version, MessageType.PUTCHUNK, this.id, fileHash, chunk.getChunkNo(), repDegree, chunk.getContents());
            MulticastChannel.multicast(message, message.length, this.mdbAddr, this.mdbPort);
            System.out.printf("MDB: chunkNo %d ; size %d%n", chunk.getChunkNo(), chunk.getSize());
        }
        return "success";
    }

    public void sendStorageResponse(String fileId, int chunkNo) {
        byte[] message = Message.createMessage(this.version, MessageType.STORED, this.id, fileId, chunkNo);
        Random rand = new Random();
        int time = rand.nextInt(400);
        try {
            Thread.sleep(time);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
        MulticastChannel.multicast(message, message.length, this.mcAddr, this.mcPort);
    }

    public static void main(String[] args) throws IOException{

        if (args.length < 1) {
            System.out.println("usage: Peer <remote_object_name>");
            throw new IOException("Invalid usage");
        }

        String version = args[0];
        int id = Integer.parseInt(args[1]);
        String service_ap = args[2];

        String MC = args[3], MDB = args[4], MDR = args[5];

        Peer peer = new Peer(version, id, MC, MDB, MDR);
        Registry registry = LocateRegistry.getRegistry();
        try {
            ClientPeerProtocol stub = (ClientPeerProtocol) UnicastRemoteObject.exportObject(peer,0);

            //Bind the remote object's stub in the registry
            registry.bind(service_ap, stub); //register peer object with the name in args[0]
        } catch (AlreadyBoundException e) {
            System.out.println("Object already bound! Rebinding...");
            registry.rebind(service_ap, peer);
        }
        System.out.println("Peer " + peer.id + " ready");

        peer.dataReceiver.start();
        peer.controlReceiver.start();
        while (true) {
            Message message = null;

            if ((message = peer.dataReceiver.getMessage()) != null) {
                Chunk chunk = new Chunk(message.fileId, message.chunkNo, message.body.length, message.replicationDegree, message.body);
                System.out.println(message.toString());
                peer.addFileEntry(message.fileId);
                peer.addChunk(message.fileId, chunk);
                chunk.store(String.valueOf(peer.id));
                peer.sendStorageResponse(message.fileId, message.chunkNo);
            }

            if ((message = peer.controlReceiver.getMessage()) != null) {
                Chunk chunk = peer.getChunk(message.fileId, message.chunkNo);
                chunk.addPerceivedReplication();
                System.out.printf("STORE from: %d  chunkNo: %d perceived: %d \n", message.senderId, message.chunkNo, chunk.getPerceivedRepDegree());
            }


        }
    }

    //Retrieved from: https://www.baeldung.com/sha-256-hashing-java
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String createFileHash(String path) {
        try {
            Path file = Paths.get(path);
            BasicFileAttributes attribs = Files.readAttributes(file, BasicFileAttributes.class); // get file metadata

            String originalString = path + attribs.lastModifiedTime() + attribs.creationTime();

            final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            final byte[] hashbytes = digest.digest(originalString.getBytes(StandardCharsets.US_ASCII));
            String fileHash = bytesToHex(hashbytes);
            this.addFileEntry(fileHash); // add file entry to files map
            return fileHash;
        }
        catch (IOException | NoSuchAlgorithmException e) {
            System.out.println(e.toString());
            System.exit(1);
        }
        return null;
    }

    private List<Chunk> createChunks(String path, String hash, int repDegree) {
        try {
            File file = new File(path);
            int num_chunks = (int) Math.floor( (double) file.length() / (double) Definitions.CHUNK_SIZE) + 1;

            FileInputStream fstream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            int num_read = fstream.read(data);
            fstream.close();

            byte[] chunk_data = new byte[Definitions.CHUNK_SIZE];
            int i = 0;
            int offset = 0;
            for (; i < num_chunks-1; i++) {
                System.arraycopy(data, offset, chunk_data, 0, Definitions.CHUNK_SIZE);
                Chunk chunk = new Chunk(hash, i, Definitions.CHUNK_SIZE, repDegree, chunk_data);
                this.addChunk(hash, chunk);
                offset += Definitions.CHUNK_SIZE;
            }

            chunk_data = Arrays.copyOfRange(data, offset, num_read);
            Chunk chunk = new Chunk(hash, i, chunk_data.length, repDegree, chunk_data);
            this.addChunk(hash, chunk);

            System.out.println("Created " + num_chunks + " chunks");
            fstream.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.getChunksOfFile(hash);
    }

    private void addFileEntry(String hash) {
        this.files.computeIfAbsent(hash, k -> new ArrayList<>());
    }

    private void addChunk(String fileHash, Chunk chunk) {
        this.files.get(fileHash).add(chunk);
    }

    private List<Chunk> getChunksOfFile(String fileHash) {
        return this.files.get(fileHash);
    }
    public Chunk getChunk(String fileId, int chunkNo) {
        List<Chunk> chunks =  this.files.get(fileId);

        for (Chunk chunk : chunks) {
            if (chunk.getChunkNo() == chunkNo)
                return chunk;
        }
        return null;
    }

}
