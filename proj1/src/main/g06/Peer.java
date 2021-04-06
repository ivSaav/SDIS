package main.g06;

import main.g06.message.Message;
import main.g06.message.MessageType;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;


public class Peer implements ClientPeerProtocol, Serializable {

    private final int id;
    private final String version;
    private int disk_usage; //disk usage in KBytes
    private Map<String, String> filenameHashes; // filename --> fileHash
    private Map<String, FileDetails> initiatedFiles; // filehash --> FileDetail
    private Map<String, FileDetails> storedFiles; // filehash --> Chunks

    private final MulticastChannel backupChannel;
    private final MulticastChannel controlChannel;

    private boolean hasChanges; //if current state is saved or not

    public Peer(String version, int id, String MC, String MDB, String MDR) {
        this.id = id;
        this.version = version;
        this.disk_usage = 0;
        // TODO: Reload this data from disk
        this.filenameHashes = new ConcurrentHashMap<>();
        this.initiatedFiles = new ConcurrentHashMap<>();
        this.storedFiles = new ConcurrentHashMap<>();

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

        this.hasChanges = false;
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
            this.filenameHashes.put(path, fileHash);
            this.initiatedFiles.put(fileHash, fd);

            this.hasChanges = true; // flag for peer backup

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

                fd.addChunk(new Chunk(fileHash, chunkNo, num_read));
                System.out.printf("MDB: chunkNo %d ; size %d\n", chunkNo, num_read);

                // TODO: Repeat message N times if rep degree was not reached
                // TODO: Advance chunk only after rep degree was reached

                last_num_read = num_read;
                chunkNo++;
            }

            System.out.println(this.initiatedFiles);
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
        if (this.filenameHashes.containsKey(file)) {

            String hash = this.filenameHashes.get(file);
            // send delete message to other peers
            FileDetails fileInfo = initiatedFiles.get(hash);
            byte[] message = Message.createMessage(this.version, MessageType.DELETE, this.id, fileInfo.getHash());
            controlChannel.multicast(message, message.length);
            System.out.printf("DELETE %s\n", file);

            //remove all data regarding this file
//            this.removeFile(file);
            this.disk_usage -= fileInfo.getSize() / 1000;
            this.removeInitiatedFile(file);

            this.hasChanges = true; // flag for peer backup
            return "success";
        }
        return "";
    }

    @Override
    public String reclaim(int new_capacity) {

        List<FileDetails> stored = new ArrayList<>(this.storedFiles.values());

        System.out.println(this.storedFiles);

        while (this.disk_usage > new_capacity) {
            FileDetails file = stored.get(0);
                for (Chunk chunk : file.getChunks()) {
                    this.disk_usage -= chunk.getSize() / 1000;

                    file.removeChunk(chunk.getChunkNo()); // remove chunk from file
                    chunk.removeStorage(this.id); // remove storage

                    // TODO: remove this chunk from the stored chunks
                    byte[] message = Message.createMessage(this.version, MessageType.REMOVED, this.id, chunk.getFilehash(), chunk.getChunkNo());
                    controlChannel.multicast(message, message.length);
                    this.setChangesFlag();

                    if (this.disk_usage <= new_capacity)
                        return "success";
                }
        }

        return "success";
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
        PeerRecovery recovery = new PeerRecovery(peer); //recover previously saved peer data

        System.out.println(peer);

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

        // save current peer state every 30 seconds
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(recovery, 15, 30, TimeUnit.SECONDS);
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

    public Map<String, FileDetails> getStoredFiles() {
        return storedFiles;
    }

    public void clearChangesFlag() {
        this.hasChanges = false;
    }

    public boolean hasChanges() {
        return hasChanges;
    }

    public void setChangesFlag() {
        this.hasChanges = true;
    }

    public void addPerceivedReplication(int peerId, String fileHash, int chunkNo) {

        // for files initiated by this peer
        if (this.initiatedFiles.containsKey(fileHash))
            this.initiatedFiles.get(fileHash).addChunkReplication(chunkNo, peerId);

        // for file stored by this peer
        if (this.storedFiles.containsKey(fileHash))
            this.storedFiles.get(fileHash).addChunkReplication(chunkNo, peerId);
    }


    // TODO: Do synchronized stuff
    public void addStoredChunk(Chunk chunk, int desiredReplication) {
        FileDetails file = this.storedFiles.computeIfAbsent(chunk.getFilehash(), v -> new FileDetails(chunk.getFilehash(),0, desiredReplication));
        this.disk_usage += chunk.getSize() / 1000; // update current disk space usage
        file.addChunk(chunk);
    }

    public Chunk getFileChunk(String fileHash, int chunkNo) {

        //find chunk in stored chunks list
        FileDetails file = this.storedFiles.get(fileHash);

        if (file != null)
            file = this.initiatedFiles.get(fileHash);


        return file == null ? null : file.getChunk(chunkNo);
    }

    public int getFileReplication(String fileHash) {
        //find chunk in stored chunks list
        FileDetails file = this.storedFiles.get(fileHash);

        if (file != null)
            file = this.initiatedFiles.get(fileHash);

        return file == null ? 0 : file.getDesiredReplication();
    }

    public void restoreState(Peer previous) {
        this.storedFiles = previous.storedFiles;
        this.initiatedFiles = previous.initiatedFiles;
        this.filenameHashes = previous.filenameHashes;
    }

    public void removeInitiatedFile(String filename) {
        String hash = this.filenameHashes.get(filename);

        this.initiatedFiles.remove(hash);
//        this.storedChunks.remove(filename);
        this.filenameHashes.remove(filename);
    }

    public void removeStoredFile(String hash) {
        this.storedFiles.remove(hash);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "id=" + id +
                ", version='" + version + '\'' +
                ", fileHashes=" + filenameHashes +
                ", fileDetails=" + initiatedFiles +
                ", storedChunks=" + storedFiles +
                ", backupChannel=" + backupChannel +
                ", controlChannel=" + controlChannel +
                '}';
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(this.filenameHashes);
        out.writeObject(this.initiatedFiles);
        out.writeObject(this.storedFiles);
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.filenameHashes = (ConcurrentHashMap<String, String>) in.readObject();
        this.initiatedFiles = (ConcurrentHashMap<String, FileDetails>) in.readObject();
        this.storedFiles = (ConcurrentHashMap<String, FileDetails>) in.readObject();
    }
}
