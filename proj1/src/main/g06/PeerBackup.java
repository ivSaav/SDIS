package main.g06;

import main.g06.Definitions;
import main.g06.Peer;

import java.io.*;

public class PeerBackup extends Thread {
    private final Peer peer;

    public PeerBackup(Peer peer) {
        this.peer = peer;

    }

    public void saveState() {
        this.start();
    }

    @Override
    public void run() {
        this.backupData();
    }

    // TODO create thread to perform this operation
    private void backupData() {
        try {
            String filename = Definitions.STORAGE_DIR + File.separator + peer.getId() + File.separator + "backup.ser";
            File file = new File(filename);

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            FileOutputStream fstream = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fstream);

            oos.writeObject(this.peer);
            oos.flush();
            oos.close();
            fstream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
