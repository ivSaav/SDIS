package main.g06;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class FileDetails implements Serializable {
    private String hash;
    private long size;
    private int desiredRepDegree;

    private List<Set<Integer>> chunks;

    public FileDetails(String hash, long size, int desiredRepDegree) {
        this.hash = hash;
        this.size = size;
        this.desiredRepDegree = desiredRepDegree;

        this.chunks = new ArrayList<>();

        // number of chunks necessary to backup this file
        int num_chunks = (int) Math.floor( (double) size / (double) Definitions.CHUNK_SIZE) + 1;
        for (int i = 0; i < num_chunks; i++) {
            this.chunks.add(new HashSet<>());
        }
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }

    public int getDesiredRepDegree() {
        return desiredRepDegree;
    }

    public int getChunkPerceivedRepDegree(int chunkNo) {
        return chunks.get(chunkNo).size();
    }

    public boolean addChunkPeer(int chunkNo, int peerId) {
        return chunks.get(chunkNo).add(peerId);
    }

    public void resetChunkOwners(int chunkNo) {
        chunks.get(chunkNo).clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof String) return this.hash.equals(o);
        if (!(o instanceof FileDetails)) return false;
        FileDetails that = (FileDetails) o;
        return hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash);
    }

    @Override
    public String toString() {
        return "FileDetails{" +
                "hash='" + hash + '\'' +
                ", size=" + size +
                ", desiredRepDegree=" + desiredRepDegree +
                ", chunks=" + chunks +
                '}';
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(this.hash);
        out.writeLong(this.size);
        out.writeInt(this.desiredRepDegree);
        out.writeObject(this.chunks);
    }
    @Serial
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.hash = in.readUTF();
        this.size = in.readLong();
        this.desiredRepDegree = in.readInt();
        this.chunks = (List<Set<Integer>>) in.readObject();
    }
}
