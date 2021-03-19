import java.io.IOException;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

    private TestApp() {}

    public static void main(String[] args) {


//        ArgsParser.validateArguments(args);

        try {
            Registry registry = LocateRegistry.getRegistry(8001);
            RMI stub = (RMI) registry.lookup("RMI");

            String response;
            if (args[2].equals("BACKUP")) {
                response = stub.register(args[3], args[4]);
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
