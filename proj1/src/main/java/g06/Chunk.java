package main.java.g06;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public void store(int peerId) {

        String filename = this.fileId + "_" + this.chunkNo;
        try {
            File file = new File(".." + File.separator + "storage" + File.separator + peerId + File.separator + filename);
            file.getParentFile().mkdir();
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            out.write(this.contents);
            out.flush();
            out.close();
        }
        catch (IOException e) {
            System.out.println("Couldn't locate specified file " + filename);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean removeStorage(int peerId) {
        String filename = this.fileId + "_" + this.chunkNo;
        File file = new File(".." + File.separator + "storage" + File.separator + peerId + File.separator + filename);
        return file.delete();
    }
}