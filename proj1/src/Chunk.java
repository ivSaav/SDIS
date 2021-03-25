public class Chunk {
    private final String fileId;
    private final int chunkNo;
    private final int size;
    private final int desiredRepDegree;
    private int perceivedRepDegree;
    private final byte[] contents;

    public Chunk(String fileId, int chunkNo, int size, int desiredRepDegree, byte[] contents) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.size = size;
        this.desiredRepDegree = desiredRepDegree;
        this.perceivedRepDegree = 0;
        this.contents = contents;
    }

    public byte[] getContents() { return this.contents; }
    public void addPerceivedReplication() { this.perceivedRepDegree++; }
    public int getChunkNo() { return this.chunkNo; }
    public int getSize() { return this.size; }
    public int getPerceivedRepDegree() { return this.perceivedRepDegree; }
}