package solution;
import java.rmi.*;

public interface CommandServerProtocol extends Remote{

    public void SET(String name) throws RemoteException;
    public void START() throws RemoteException;
    public float RESULT(byte[] result, ClientProtocol client) throws RemoteException;
}
