package main.g06;

import java.io.*;

public class PeerRecovery implements Runnable {
    private final Peer peer;

    public PeerRecovery(Peer peer) {
        this.peer = peer;
        this.recoverData();
    }

    @Override
    public void run() {
        this.backupData();
    }

    private void backupData() {
        try {
            if (!peer.hasChanges())
                return;
            System.out.println("Saving current state");
            String filename = Definitions.STORAGE_DIR + File.separator + peer.getId() + File.separator + "backup.ser";
            File file = new File(filename);

            if (!file.exists()) { //create missing directories
                file.getParentFile().mkdirs();
                file.createNewFile();
            }


            FileOutputStream fstream = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fstream);

            // save peer object
            oos.writeObject(this.peer);
            oos.flush();
            oos.close();
            fstream.close();

            this.peer.clearChangesFlag(); // changes saved locally
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recoverData() {
        // recover peer's backed up state
        String filename = Definitions.STORAGE_DIR + File.separator + this.peer.getId() + File.separator + "backup.ser";
        try {
            File file = new File(filename);
            if (!file.exists()) // didn't find a backed up version
                return;
            System.out.println("Recovering last saved state");

            // fetch backed up data
            FileInputStream fstream = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fstream);
            Peer previous_version = (Peer) ois.readObject(); // reading object from serialized file

            ois.close();
            fstream.close();

            // copying backed data
            this.peer.restoreState(previous_version);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
