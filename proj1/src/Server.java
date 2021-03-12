import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Server implements RMI {
    public Server() {}

    private HashMap<String, String> table = new HashMap<>();

    @Override
    public String register(String dns, String ipAddr) throws RemoteException {

        System.out.println("register " + dns + " " + ipAddr);
        if (table.containsKey(dns)) {
            System.out.println("ERROR a table entry already exists");
            return "ERROR register already exists";
        }
        table.put(dns, ipAddr);
        System.out.println("registered: " + dns + ":" + ipAddr);
        return "success";
    }

    @Override
    public String lookup(String dns) throws RemoteException {
        System.out.println("lookup " + dns);
        String val = table.get(dns);
        if (val == null) {
            System.out.println("ERROR no entry for " + dns);
            return "ERROR no entry for " + dns;
        }
        System.out.println("lookup " + dns);
        return val;
    }

    public static void main(String[] args) throws IOException{

        try {
            Server obj = new Server();
            RMI stub = (RMI) UnicastRemoteObject.exportObject(obj, 8002);

            //Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(8001);
            registry.bind("RMI", stub);

            System.out.println("Server ready");
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }

        if (args.length < 1) {
            System.out.println("usage: Server <remote_object_name>");
            throw new IOException("Invalid usage");
        }
    }

}
