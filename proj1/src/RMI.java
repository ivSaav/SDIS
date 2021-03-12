import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI extends Remote {
    String register(String dns, String ipAddr) throws RemoteException;
    String lookup(String dns) throws RemoteException;
}