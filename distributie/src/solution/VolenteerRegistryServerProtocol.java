package solution;
import java.rmi.*;

public interface VolenteerRegistryServerProtocol extends Remote {

    public void ADD(ClientProtocol stub) throws RemoteException;
    public void ADD(CommandServerProtocol stub) throws RemoteException;
    public void REMOVE(ClientProtocol client) throws RemoteException;
    public void REMOVE(CommandServerProtocol server) throws RemoteException;
    public ClientProtocol GET() throws RemoteException;
    public void SET(ClientProtocol client) throws RemoteException;

}
