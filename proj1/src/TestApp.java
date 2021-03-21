import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

    private TestApp() {}

    public static void main(String[] args) {


//        ArgsParser.validateArguments(args);
        String peer_ap = args[0];
        String filename = args[2];
        int repDegree = Integer.parseInt(args[3]);
        Definitions.Operation operation = Definitions.Operation.valueOf(args[1]);

        try {
            Registry registry = LocateRegistry.getRegistry(8001);
            ClientPeerProtocol stub = (ClientPeerProtocol) registry.lookup(peer_ap);

            String response;
            if (operation == Definitions.Operation.BACKUP) {
                response = stub.backup(filename, repDegree);
            }
            else {
                System.out.println("ERROR invalid operation:" + args[2]);
                return;
            }
            System.out.println("response: " + response);

        } catch (NotBoundException | RemoteException e) {
            e.printStackTrace();
        }


    }
}
