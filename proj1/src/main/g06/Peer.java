package main.g06;

import main.g06.message.ChunkMonitor;
import main.g06.message.Message;
import main.g06.message.MessageType;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;


public class Peer implements ClientPeerProtocol, Serializable {

    private static final String restoreDirectory = "restored" + File.separator;

    private final int id;
    private final String version;
    private int max_space;
    private int disk_usage; //disk usage in KBytes
    private Map<String, String> filenameHashes; // filename --> fileHash
    private Map<String, FileDetails> initiatedFiles; // filehash --> FileDetail
    private Map<String, FileDetails> storedFiles; // filehash --> Chunks

    private final MulticastChannel backupChannel;
    private final MulticastChannel restoreChannel;
    private final MulticastChannel controlChannel;

    private boolean hasChanges; //if current state is saved or not

    public Peer(String version, int id, String MC, String MDB, String MDR) {
        this.id = id;
        this.version = version;
        this.max_space = Integer.MAX_VALUE; // unlimited storage space in the beginning
        this.disk_usage = 0; // current used space
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
        this.restoreChannel = new MulticastChannel(this, mdrAddr, mdrPort, (ThreadPoolExecutor) Executors.newFixedThreadPool(10));

        this.hasChanges = false;
    }

    @Override
    public String backup(String path, int repDegree) {
        if (this.filenameHashes.containsKey(path)) { // checking for a previous version of this file
            System.out.println("Found previous version of: " + path);
            this.delete(path); // removing previous version
        }

        String fileHash = SdisUtils.createFileHash(path);
        if (fileHash == null)
            return "failure";

        try {
            File file = new File(path);
            int num_chunks = (int) Math.floor( (double) file.length() / (double) Definitions.CHUNK_SIZE) + 1;

            // Save filename and its generated hash
            FileDetails fd = new FileDetails(fileHash, file.length(), repDegree);
            this.filenameHashes.put(path, fileHash);
            this.initiatedFiles.put(fileHash, fd);

            FileInputStream fstream = new FileInputStream(file);
            byte[] chunk_data = new byte[Definitions.CHUNK_SIZE];
            int num_read, last_num_read = -1, chunkNo = 0;

            // Read file chunks and send them
            while ((num_read = fstream.read(chunk_data)) != -1) {
                // Send message
                byte[] message = num_read == Definitions.CHUNK_SIZE ?
                        Message.createMessage(this.version, MessageType.PUTCHUNK, this.id, fileHash, chunkNo, repDegree, chunk_data)
                        : Message.createMessage(this.version, MessageType.PUTCHUNK, this.id, fileHash, chunkNo, repDegree, Arrays.copyOfRange(chunk_data, 0, num_read));

                fd.addChunk(new Chunk(fileHash, chunkNo, num_read));

                int max_putchunk_tries = 5;
                int attempts = 0;
                while (attempts < max_putchunk_tries) {
                    System.out.printf("MDB: chunkNo %d ; size %d\n", chunkNo, num_read);
                    backupChannel.multicast(message, message.length);
                    ChunkMonitor monitor = fd.addMonitor(chunkNo);
                    if (monitor.await_receive())
                        break;
                    System.out.println("[!] Couldn't achieve desired replication. Resending...");
                    attempts++;
                }
                last_num_read = num_read;
                chunkNo++;
            }

            // Case of last chunk being size 0
            if (last_num_read == Definitions.CHUNK_SIZE) {
                byte[] message = Message.createMessage(this.version, MessageType.PUTCHUNK, this.id, fileHash, chunkNo, repDegree, new byte[] {});
                backupChannel.multicast(message, message.length);
                System.out.printf("MDB: chunkNo %d ; size %d\n", chunkNo, num_read);
            }

            fd.clearMonitors();

            fstream.close();
            System.out.println("Created " + num_chunks + " chunks");
            this.hasChanges = true; // flag for peer backup
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
            this.disk_usage -= fileInfo.getSize() / 1000;
            this.removeInitiatedFile(file);

            this.hasChanges = true; // flag for peer backup
            return "success";
        }
        return "";
    }

    @Override
    public String reclaim(int new_capacity) {
        System.out.println("RECLAIM max_size: " + new_capacity);

        this.max_space = new_capacity;

        List<FileDetails> stored = new ArrayList<>(this.storedFiles.values());

        while (this.disk_usage > this.max_space) {
            FileDetails file = stored.remove(0);
                for (Chunk chunk : file.getChunks()) {
                    this.disk_usage -= chunk.getSize() / 1000;

                    file.removeChunk(chunk.getChunkNo()); // remove chunk from file
                    chunk.removeStorage(this.id); // remove storage

                    file.getChunks().remove(chunk); // remove chunk from stored file

                    byte[] message = Message.createMessage(this.version, MessageType.REMOVED, this.id, chunk.getFilehash(), chunk.getChunkNo());
                    controlChannel.multicast(message, message.length);
                    this.setChangesFlag();

                    if (this.disk_usage <= this.max_space)
                        return "success";
                }
        }
        return "success";
    }

    @Override
    public String restore(String file) throws RemoteException {
        // checking if this peer was the initiator for file backup
        if (this.filenameHashes.containsKey(file)) {

            String hash = this.filenameHashes.get(file);
            // send delete message to other peers
            FileDetails fileInfo = initiatedFiles.get(hash);

            File restored = new File(restoreDirectory + this.id + File.separator + file);
            FileOutputStream fstream;
            restored.getParentFile().mkdirs();

            try {
                restored.createNewFile();
                fstream = new FileOutputStream(restored);

                byte[] message;
                boolean lastChunk = false;
                int chunkNo = 0;
                while (!lastChunk) {
                    ChunkMonitor cm = fileInfo.addMonitor(chunkNo);
                    message = Message.createMessage(this.version, MessageType.GETCHUNK, this.id, fileInfo.getHash(), chunkNo);

                    int i;
                    for (i = 0; i < 3; i++) {  // 3 retries per chunk
                        // send GETCHUNK message to other peers
                        controlChannel.multicast(message, message.length);
                        System.out.printf("RESTORE %s %d\n", file, chunkNo);

                        if (!cm.await_receive())
                            continue;

                        fileInfo.removeMonitor(chunkNo);

                        fstream.write(cm.getData());

                        if (cm.getData().length != Definitions.CHUNK_SIZE)
                            lastChunk = true;
                        break;
                    }

                    if (i >= 3) {
                        fstream.close();
                        return "failure";
                    }

                    chunkNo++;
                }

                fstream.close();

            } catch (IOException e) {
                e.printStackTrace();
                return "failure";
            }
        }

        return "success";
    }

    public String state() throws RemoteException {
        StringBuilder ret = new StringBuilder("\n========== INFO ==========\n");

        ret.append(String.format("peerID: %d \nversion: %s \nmax capacity: %d KB\nused: %d KB\n", this.getId(), this.version, this.max_space, this.disk_usage));

        if (!this.initiatedFiles.isEmpty()) {
            ret.append("\n========== INITIATED ===========\n");
            for (Map.Entry<String, String> entry : this.filenameHashes.entrySet()) {
                String filename = entry.getKey();
                String hash = entry.getValue();
                FileDetails fd = this.initiatedFiles.get(hash);
                ret.append(
                        String.format("filename: %s \tid: %s \tdesired replication: %d\n", filename, fd.getHash(), fd.getDesiredReplication())
                );

                for (Chunk chunk : fd.getChunks()) {
                    ret.append(
                            String.format(" - chunkNo: %d \tperceived replication: %d\n", chunk.getChunkNo(), chunk.getPerceivedReplication())
                    );
                }
            }
        }

        if (!this.storedFiles.isEmpty()) {
            ret.append("\n========== STORED ==========\n");
            for (FileDetails details : this.storedFiles.values())
                for (Chunk chunk : details.getChunks())
                    ret.append(
                            String.format("chunkID: %s \tsize: %d KB\tdesired replication: %d \tperceived replication: %d\n",
                                    chunk.getFilehash() + "_" + chunk.getChunkNo(), chunk.getSize() / 1000, details.getDesiredReplication(), chunk.getPerceivedReplication()
                            ));
        }

        return ret.toString();
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

    public String getVersion() { return version; }

    public MulticastChannel getBackupChannel() { return backupChannel; }

    public MulticastChannel getControlChannel() { return controlChannel; }

    public MulticastChannel getRestoreChannel() { return restoreChannel; }

    public int getId() { return id; }

    public Map<String, FileDetails> getStoredFiles() { return storedFiles; }

    public void clearChangesFlag() { this.hasChanges = false; }

    public boolean hasChanges() { return hasChanges; }

    public void setChangesFlag() { this.hasChanges = true; }

    public FileDetails getFileDetails(String fileHash) {
        FileDetails fileDetails = this.initiatedFiles.get(fileHash);
        if (fileDetails != null)
            return fileDetails;
        return this.storedFiles.get(fileHash);
    }

    /**
     * Marks a chunk as resolved when the desired replication degree has been reached
     * Used in the backup subprotocol
     * @param fileHash - file id
     * @param chunkNo - chunk number
     */
    public void resolveInitiatedChunk(String fileHash, int chunkNo) {
        FileDetails file = this.initiatedFiles.get(fileHash);
        if (file != null){ // only used on initiator peer
            Chunk chunk = file.getChunk(chunkNo);

            if (chunk.getPerceivedReplication() >= file.getDesiredReplication()) {
                ChunkMonitor monitor = file.getMonitor(chunkNo);
                if (monitor != null)
                    monitor.markSolved();
            }
        }
    }

    /**
     * Marks a chunk as resolved when a PUTCHUNK message is received
     * Used in the REMOVED protocol
     * Prevents a PUTCHUNK message from being sent (section - 3.5 Space reclaiming subprotocol)
     * @param fileHash - file id
     * @param chunkNo - chunk number
     */
    public void resolveRemovedChunk(String fileHash, int chunkNo) {
        FileDetails file = this.storedFiles.get(fileHash);
        if (file != null) {
            ChunkMonitor monitor = file.getMonitor(chunkNo);
            if (monitor != null)
                monitor.markSolved();
        }
    }

    /**
     * Checking if there is enough space to store a given chunk
     * @param chunkSize - chunk size
     * @return boolean
     */
    public boolean hasDiskSpace(int chunkSize) {
        return (this.max_space - this.disk_usage) >= chunkSize;
    }

    /**
     * Checking if peer has already backed up this chunk
     * @param chunk - chunk beibg checked
     * @return boolean
     */
    public boolean hasStoredChunk(Chunk chunk) {
        FileDetails details = this.storedFiles.get(chunk.getFilehash());
        return details != null && (details.getChunk(chunk.getChunkNo()) != null);
    }

    public boolean isInitiator(String fileHash) {
        return this.initiatedFiles.get(fileHash) != null;
    }

    public void addPerceivedReplication(int peerId, String fileHash, int chunkNo) {
        FileDetails fileDetails = getFileDetails(fileHash);
        if (fileDetails != null)
            fileDetails.addChunkReplication(chunkNo, peerId);
    }


    // TODO: Do synchronized stuff
    public void addStoredChunk(Chunk chunk, int desiredReplication) {
        FileDetails file = this.storedFiles.computeIfAbsent(chunk.getFilehash(), v -> new FileDetails(chunk.getFilehash(),0, desiredReplication));
        this.disk_usage += chunk.getSize() / 1000; // update current disk space usage
        file.addChunk(chunk);
    }

    public void removeStoredChunk(Chunk chunk) {
        FileDetails file = this.storedFiles.get(chunk.getFilehash());
        file.removeChunk(chunk.getChunkNo());
        this.disk_usage -= chunk.getSize() / 1000;
    }

    public Chunk getFileChunk(String fileHash, int chunkNo) {

        //find chunk in stored chunks list
        FileDetails file = this.storedFiles.get(fileHash);

        if (file == null)
            file = this.initiatedFiles.get(fileHash);

        return file == null ? null : file.getChunk(chunkNo);
    }

    public int getFileReplication(String fileHash) {
        //find chunk in stored chunks list
        FileDetails file = this.storedFiles.get(fileHash);

        if (file == null)
            file = this.initiatedFiles.get(fileHash);

        return file == null ? 0 : file.getDesiredReplication();
    }

    public void restoreState(Peer previous) {
        this.disk_usage = previous.disk_usage;
        this.max_space = previous.max_space;
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
                ", max_space=" + max_space +
                ", disk_usage=" + disk_usage +
                ", filenameHashes=" + filenameHashes +
                ", initiatedFiles=" + initiatedFiles +
                ", storedFiles=" + storedFiles +
                ", backupChannel=" + backupChannel +
                ", restoreChannel=" + restoreChannel +
                ", controlChannel=" + controlChannel +
                ", hasChanges=" + hasChanges +
                '}';
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(this.disk_usage);
        out.writeInt(this.max_space);
        out.writeObject(this.filenameHashes);
        out.writeObject(this.initiatedFiles);
        out.writeObject(this.storedFiles);
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.disk_usage = in.readInt();
        this.max_space = in.readInt();
        this.filenameHashes = (ConcurrentHashMap<String, String>) in.readObject();
        this.initiatedFiles = (ConcurrentHashMap<String, FileDetails>) in.readObject();
        this.storedFiles = (ConcurrentHashMap<String, FileDetails>) in.readObject();
    }
}
