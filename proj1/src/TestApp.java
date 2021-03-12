import java.io.IOException;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {}

    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println("usage: TestApp <peer_app> <operation> <opend_1> <opend_2>");
            System.exit(1)
        }

        String message = args[0];
        int peer_app = Integer.parseInt(args[1]);

        String operation = args[1];
        String filename = "";
        int rep_degree = 0, disk_size = 0;
        if (operation.equals("BACKUP")) {
            if (args.length != 5) {
                System.out.println("usage: TestApp <peer_app> BACKUP <filename> <replication_deg>");
                System.exit(1)
            }

            filename = args[2];
            rep_degree = Integer.parseInt(args[3]);
        }
        else if (operation.equals("RESTORE")){
            if (args.length != 4) {
                System.out.println("usage: TestApp <peer_app> RESTORE <filename>");
                System.exit(1)
            }
            filename = args[2];
        }
        else if (operation.equals("DELETE")) {
            if (args.length != 4){
                System.out.println("usage: TestApp <peer_app> DELETE <filename>");
            }
            filename = args[2];
        }
        else if (operation.equals("RECLAIM")) {
            if (args.length != 2){
                System.out.println("usage: TestApp <peer_app> RECLAIM <filename>");
                System.exit(0);
                disk_size = Integer.parseInt(args[2]);
            }
        }
        else {
            System.out.println("Unknown operation");
            System.exit(0);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(8001);
            RMI stub = (RMI) registry.lookup("RMI");

            String response;
            if (args[2].equals("register")) {
                response = stub.register(args[3], args[4]);
            }
            else if (args[2].equals("lookup")) {
                response = stub.lookup(args[3]);
            }
            else {
                System.out.println("ERROR invalid operation:" + args[2]);
                return;
            }
            System.out.println("response: " + response);

        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
}
