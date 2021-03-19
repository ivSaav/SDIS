import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class Peer {


    private int id;
    private Map<String, String> files;

    public Peer(int id) {
        this.id = id;
        this.files = new HashMap<String, String>();
    }




    public String backup(String path, int repDegree) throws IOException {

        try {
            BasicFileAttributes attribs = Files.readAttributes(Path.of(path), BasicFileAttributes.class); // get file metadata

            String originalString = path + attribs.lastModifiedTime() + attribs.creationTime();

            final MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            final byte[] hashbytes = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
            String fileHash = bytesToHex(hashbytes);

//            this.files.put(fileHash, )

        }
        catch (IOException | NoSuchAlgorithmException e) {
            System.out.println(e.toString());
            System.exit(1);
        }



        // TODO files table with key as hash
//        if (table.containsKey(dns)) {
//            System.out.println("ERROR a table entry already exists");
//            return "ERROR register already exists";
//        }
//        table.put(dns, ipAddr);
        System.out.println("backed: " + path + ":" + repDegree);
        return "success";
    }

    public static void main(String[] args) throws IOException{
            Peer p = new Peer(1);
//        try {
//            Peer obj = new Peer();
//            RMI stub = (RMI) UnicastRemoteObject.exportObject(obj, 8002);
//
//            //Bind the remote object's stub in the registry
//            Registry registry = LocateRegistry.getRegistry(8001);
//            registry.bind("RMI", stub);
//
//            System.out.println("Server ready");
//        } catch (AlreadyBoundException e) {
//            e.printStackTrace();
//        }

//        if (args.length < 1) {
//            System.out.println("usage: Peer <remote_object_name>");
//            throw new IOException("Invalid usage");
//        }

        p.backup(args[0], 2);
    }

    //Retrieved from: https://www.baeldung.com/sha-256-hashing-java
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
