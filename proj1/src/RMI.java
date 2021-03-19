import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI extends Remote {
    String backup(String path, int repDegree) throws RemoteException;
//    String lookup(String dns) throws RemoteException;
}