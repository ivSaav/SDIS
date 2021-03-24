import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;


public class Peer implements ClientPeerProtocol {

    private final int id;
    private final String version;
    private Map<String, String> files;
    private InetAddress MC_addr;
    private int MC_port;
    private InetAddress MDB_addr;
    private int MDB_port;
    private InetAddress MDR_addr;
    private int MDR_port;

    public Peer(String version, int id, String MC, String MDB, String MDR) {
        this.id = id;
        this.version = version;
        this.files = new HashMap<String, String>();

        String[] vals = MC.split(":"); //MC
        try {
            this.MC_addr = InetAddress.getByName(vals[0]);
            this.MC_port = Integer.parseInt(vals[1]);

            vals = MDB.split(":");
            this.MDB_addr = InetAddress.getByName(vals[0]);
            this.MDB_port = Integer.parseInt(vals[1]);

            vals = MDR.split(":");
            this.MDR_addr = InetAddress.getByName(vals[0]);
            this.MDR_port = Integer.parseInt(vals[1]);
        }
        catch (UnknownHostException e) {
            System.out.println("Invalid Hostname " + vals[0]);
            System.exit(1);
        }
    }

    @Override
    public String backup(String path, int repDegree) {
        try {
            Path file = Paths.get(path);
            BasicFileAttributes attribs = Files.readAttributes(file, BasicFileAttributes.class); // get file metadata

            String originalString = path + attribs.lastModifiedTime() + attribs.creationTime();

            final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            final byte[] hashbytes = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
            String fileHash = bytesToHex(hashbytes);

            System.out.println(fileHash);

        }
        catch (IOException | NoSuchAlgorithmException e) {
            System.out.println(e.toString());
            System.exit(1);
        }

        System.out.println("backed: " + path + ":" + repDegree);
        return "success";
    }

    public static void main(String[] args) throws IOException{

        if (args.length < 1) {
            System.out.println("usage: Peer <remote_object_name>");
            throw new IOException("Invalid usage");
        }

        String version = args[0];
        int id = Integer.parseInt(args[1]);
        String service_ap = args[2];

        String MC = args[3], MDB = args[4], MDR = args[5];

        Peer peer = new Peer(version, id, MC, MDB, MDR);
        Registry registry = LocateRegistry.getRegistry();
        try {
            ClientPeerProtocol stub = (ClientPeerProtocol) UnicastRemoteObject.exportObject(peer,0);

            //Bind the remote object's stub in the registry
            registry.bind(service_ap, stub); //register peer object with the name in args[0]
        } catch (AlreadyBoundException e) {
            System.out.println("Object already bound! Rebinding...");
            registry.rebind(service_ap, peer);
        }
        System.out.println("Peer " + peer.id + " ready");
    }

    //Retrieved from: https://www.baeldung.com/sha-256-hashing-java
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void splitFileChunks(String path) {
        try {

            File file = new File(path);
            int num_chunks = (int) Math.ceil( (double) file.length() / (double) Definitions.CHUNK_SIZE);

            byte[] chunk_bytes = new byte[Definitions.CHUNK_SIZE];
            FileInputStream fstream = new FileInputStream(file);

            for (int i = 0; i < num_chunks; i++) {
                int offset = i*num_chunks;
                int num_read = fstream.read(chunk_bytes, offset, offset + Definitions.CHUNK_SIZE);
                System.out.println(num_read);
            }

            fstream.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
