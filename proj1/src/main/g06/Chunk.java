package main.g06;

import java.io.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Chunk implements Serializable {
    private String filehash;
    private int chunkNo;
    private int size;
    private Set<Integer> replications; // set with ids of the peers who have a replication of this chunk


    public Chunk(String filehash, int chunkNo, int size) {
        this.filehash = filehash;
        this.chunkNo = chunkNo;
        this.size = size;
        this.replications = new HashSet<>();
    }

    public String getFilehash() { return filehash; }
    public int getChunkNo() { return this.chunkNo; }
    public int getSize() { return this.size; }


    public synchronized int getPerceivedReplication() {
        return this.replications.size();
    }

    public synchronized void addReplication(int peerId) {
        this.replications.add(peerId);
    }

    public synchronized void removeReplication(int peerId) {
        this.replications.remove(peerId);
    }

    public void store(int peerId, byte[] contents) {

        String filename = this.filehash + "_" + this.chunkNo;
        try {
            File file = new File("storage" + File.separator + peerId + File.separator + filename);
            file.getParentFile().mkdirs();
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            out.write(contents);
            out.flush();
            out.close();

            this.addReplication(peerId);
        }
        catch (IOException e) {
            System.out.println("Couldn't locate specified file " + filename);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public byte[] retrieve(int peerId) {
        // fetch backed up chunk
        String filename = "storage" + File.separator + peerId + File.separator + this.filehash + "_" + this.chunkNo;
        byte[] body = new byte[this.size];
        File file = new File(filename);
        try {
            FileInputStream fstream = new FileInputStream(file);
            int num_read = fstream.read(body);
            fstream.close();
        } catch (FileNotFoundException e) {
            System.out.println("Couldn't locate file (retrieve)");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return body;
    }

    public boolean removeStorage(int peerId) {
        String filename = this.filehash + "_" + this.chunkNo;
        File file = new File("storage" + File.separator + peerId + File.separator + filename);
        if (!file.exists())
            System.out.printf("Couldn't locate %s \n", filename);
        return file.delete();
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "filehash='" + filehash.substring(0, 5) + '\'' +
                ", chunkNo=" + chunkNo +
                ", size=" + size +
                ", replications=" + replications +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chunk)) return false;
        Chunk chunk = (Chunk) o;
        return chunkNo == chunk.chunkNo && filehash.equals(chunk.filehash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filehash, chunkNo);
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(this.filehash);
        out.writeInt(this.chunkNo);
        out.writeInt(this.size);
        out.writeObject(this.replications);
    }

    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.filehash = in.readUTF();
        this.chunkNo = in.readInt();
        this.size = in.readInt();
        this.replications = (Set<Integer>) in.readObject();
    }
}