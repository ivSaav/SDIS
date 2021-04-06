package main.g06;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class FileDetails implements Serializable {
    private String hash;
    private long size;
    private int desiredRepDegree;

    private Map<Integer, Chunk> chunks;

    public FileDetails(String hash, long size, int desiredRepDegree) {
        this.hash = hash;
        this.size = size;
        this.desiredRepDegree = desiredRepDegree;

        this.chunks = new HashMap<>();
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }

    public synchronized void addChunk(Chunk chunk) {
        this.chunks.put(chunk.getChunkNo(), chunk);
    }

   public synchronized void removeChunk(int chunkNo) {
       this.chunks.remove(chunkNo);
   }

    public int getDesiredReplication() {
        return desiredRepDegree;
    }

    public synchronized List<Chunk> getChunks() {
        return (List<Chunk>) chunks.values();
    }

    public synchronized Chunk getChunk(int chunkNo) {
        return this.chunks.get(chunkNo);
    }

    public synchronized int getChunkReplication(int chunkNo) {
        return chunks.get(chunkNo).getPerceivedReplication();
    }

    public synchronized void addChunkReplication(int chunkNo, int peerId) {
        chunks.get(chunkNo).addReplication(peerId);
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
                "hash='" + hash.substring(0,5) + '\'' +
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
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.hash = in.readUTF();
        this.size = in.readLong();
        this.desiredRepDegree = in.readInt();
        this.chunks = (Map<Integer, Chunk>) in.readObject();
    }
}
