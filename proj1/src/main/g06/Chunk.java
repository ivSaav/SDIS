package main.g06;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class Chunk implements Serializable {
    private String filehash;
    private int chunkNo;
    private int size;

    public Chunk(String filehash, int chunkNo, int size, int desiredRepDegree) {
        this.filehash = filehash;
        this.chunkNo = chunkNo;
        this.size = size;
    }



    public String getFilehash() { return filehash; }
    public int getChunkNo() { return this.chunkNo; }
    public int getSize() { return this.size; }

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
        }
        catch (IOException e) {
            System.out.println("Couldn't locate specified file " + filename);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean removeStorage(int peerId) {
        String filename = this.filehash + "_" + this.chunkNo;
        File file = new File("storage" + File.separator + peerId + File.separator + filename);
        if (!file.exists())
            System.out.printf("Couldn't locate %s \n", filename);
        return file.delete();
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

    @Override
    public String toString() {
        return "Chunk{" +
                "filehash='" + filehash + '\'' +
                ", chunkNo=" + chunkNo +
                ", size=" + size +
                '}';
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(this.filehash);
        out.writeInt(this.chunkNo);
        out.writeInt(this.size);
    }
    @Serial
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.filehash = in.readUTF();
        this.chunkNo = in.readInt();
        this.size = in.readInt();
    }
}