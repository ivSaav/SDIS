package main.java.g06;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileDetails {
    private final String hash;
    private final long size;
    private final int desiredRepDegree;

    private final List<Set<Integer>> chunks;

    public FileDetails(String hash, long size, int desiredRepDegree) {
        this.hash = hash;
        this.size = size;
        this.desiredRepDegree = desiredRepDegree;

        this.chunks = new ArrayList<>();
        for (int i = 0; i < size / Definitions.CHUNK_SIZE; i++) {
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
}
