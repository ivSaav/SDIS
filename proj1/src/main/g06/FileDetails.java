package main.g06;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class FileDetails implements Serializable {
    private String hash;
    private long size;
    private int desiredRepDegree;

    private Map<Integer, Chunk> chunks;

    public FileDetails(String hash, long size, int desiredRepDegree) {
        this.hash = hash;
        this.size = size;
        this.desiredRepDegree = desiredRepDegree;

        this.chunks = new ConcurrentHashMap<>();
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }

    public void addChunk(Chunk chunk) {
        this.chunks.put(chunk.getChunkNo(), chunk);
    }

   public void removeChunk(int chunkNo) {
       this.chunks.remove(chunkNo);
   }

    public int getDesiredReplication() {
        return desiredRepDegree;
    }
    public Collection<Chunk> getChunks() {
        return chunks.values();
    }

    public Chunk getChunk(int chunkNo) {
        return this.chunks.get(chunkNo);
    }

    public int getChunkReplication(int chunkNo) {
        return chunks.get(chunkNo).getPerceivedReplication();
    }

    public void addChunkReplication(int chunkNo, int peerId) {
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
        this.chunks = (ConcurrentHashMap<Integer, Chunk>) in.readObject();
    }
}
