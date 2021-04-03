package main.java.g06;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientPeerProtocol extends Remote {
    String backup(String path, int repDegree) throws RemoteException;
    String delete(String file) throws RemoteException;
}