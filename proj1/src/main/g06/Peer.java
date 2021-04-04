package main.g06;

import main.g06.message.Message;
import main.g06.message.MessageType;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class Peer implements ClientPeerProtocol, Serializable {

    private final int id;
    private final String version;
    private Map<String, FileDetails> fileHashes; // filename --> FileDetail
    private Map<String, FileDetails> fileDetails; // filehash --> FileDetail
    private Map<String, Set<Chunk>> storedChunks; // filehash --> Chunks

    private final MulticastChannel backupChannel;
    private final MulticastChannel controlChannel;

    public Peer(String version, int id, String MC, String MDB, String MDR) {
        this.id = id;
        this.version = version;
        // TODO: Reload this data from disk
        this.fileHashes = new HashMap<>();
        this.fileDetails = new HashMap<>();
        this.storedChunks = new HashMap<>();

        this.recoverData();

        String[] vals = MC.split(":"); //MC

        String mcAddr = vals[0];
        int mcPort = Integer.parseInt(vals[1]);

        vals = MDB.split(":");
        String mdbAddr = vals[0];
        int mdbPort = Integer.parseInt(vals[1]);

        vals = MDR.split(":");
        String mdrAddr = vals[0];
        int mdrPort = Integer.parseInt(vals[1]);

        // TODO: Use custom ThreadPoolExecutor to maximize performance
        this.backupChannel = new MulticastChannel(this, mdbAddr, mdbPort, (ThreadPoolExecutor) Executors.newFixedThreadPool(10));
        this.controlChannel = new MulticastChannel(this, mcAddr, mcPort, (ThreadPoolExecutor) Executors.newFixedThreadPool(10));
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
            FileDetails fd = new FileDetails(fileHash, file.length(), repDegree);
            this.fileHashes.put(path, fd);
            this.fileDetails.put(fileHash, fd);

            FileInputStream fstream = new FileInputStream(file);
            byte[] chunk_data = new byte[Definitions.CHUNK_SIZE];
            int num_read, last_num_read = -1, chunkNo = 0;

            // Read file chunks and send them
            while ((num_read = fstream.read(chunk_data)) != -1) {
                // Send message
                byte[] message = num_read == Definitions.CHUNK_SIZE ?
                        Message.createMessage(this.version, MessageType.PUTCHUNK, this.id, fileHash, chunkNo, repDegree, chunk_data)
                        : Message.createMessage(this.version, MessageType.PUTCHUNK, this.id, fileHash, chunkNo, repDegree, Arrays.copyOfRange(chunk_data, 0, num_read));
                backupChannel.multicast(message, message.length);
                System.out.printf("MDB: chunkNo %d ; size %d\n", chunkNo, num_read);

                // TODO: Repeat message N times if rep degree was not reached
                // TODO: Advance chunk only after rep degree was reached

                last_num_read = num_read;
                chunkNo++;
            }

            // Case of last chunk being size 0
            if (last_num_read == Definitions.CHUNK_SIZE) {
                byte[] message = Message.createMessage(this.version, MessageType.PUTCHUNK, this.id, fileHash, chunkNo, repDegree, new byte[] {});
                backupChannel.multicast(message, message.length);
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
    public String delete(String file) {
        // checking if this peer was the initiator for file backup
        if (this.fileHashes.containsKey(file)) {
            // send delete message to other peers
            FileDetails fileInfo = fileHashes.get(file);
            byte[] message = Message.createMessage(this.version, MessageType.DELETE, this.id, fileInfo.getHash());
            controlChannel.multicast(message, message.length);
            System.out.printf("DELETE %s\n", file);

            //remove all data regarding this file
//            this.removeFile(file);

            return "success";
        }
        return "";
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

        peer.backupChannel.start();
        peer.controlChannel.start();
    }

    public String getVersion() {
        return version;
    }

    public MulticastChannel getBackupChannel() {
        return backupChannel;
    }

    public MulticastChannel getControlChannel() {
        return controlChannel;
    }

    public int getId() {
        return id;
    }

    public Map<String, Set<Chunk>> getStoredChunks() {
        return storedChunks;
    }

    public void addPerceivedReplication(int peer_id, String fileHash, int chunkNo) {
        FileDetails file = this.fileDetails.get(fileHash);
        file.addChunkPeer(chunkNo, peer_id);
        this.backupData();
    }

    // TODO: Do synchronized stuff
    public void addStoredChunk(Chunk chunk) {
        Set<Chunk> fileChunks = this.storedChunks.computeIfAbsent(
                chunk.getFilehash(),
                l -> new HashSet<>()
        );
        fileChunks.add(chunk);
    }

    public void backupData() {
        try {
            FileOutputStream fstream = new FileOutputStream("file" + this.id + ".ser");
            ObjectOutputStream oos = new ObjectOutputStream(fstream);

            oos.writeObject(this);
            oos.flush();
            oos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recoverData() {
        try {
            File f = new File("file" + this.id  + ".ser");
            if (!f.exists())
                return;
            FileInputStream fstream = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fstream);
            Peer previous_version = (Peer) ois.readObject(); // reading object from serialized file

            // recovering missing data
            this.storedChunks = previous_version.storedChunks;
            this.fileHashes = previous_version.fileHashes;
            this.fileDetails = previous_version.fileDetails;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void removeFile(String filename) {
        String hash = this.fileHashes.get(filename).getHash();

        this.fileDetails.remove(hash);
        this.storedChunks.remove(filename);
        this.fileHashes.remove(filename);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "id=" + id +
                ", version='" + version + '\'' +
                ", fileHashes=" + fileHashes +
                ", fileDetails=" + fileDetails +
                ", storedChunks=" + storedChunks +
                ", backupChannel=" + backupChannel +
                ", controlChannel=" + controlChannel +
                '}';
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(this.fileHashes);
        out.writeObject(this.fileDetails);
        out.writeObject(this.storedChunks);
    }
    @Serial
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.fileHashes = (Map<String, FileDetails>) in.readObject();
        this.fileDetails = (Map<String, FileDetails>) in.readObject();
        this.storedChunks = (Map<String, Set<Chunk>>) in.readObject();
    }
}
