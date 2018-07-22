package solution;
import data.models.Comment;

import java.rmi.*;
import java.security.PublicKey;
import java.util.List;

public interface ClientProtocol extends Remote {

    public int SIZE() throws RemoteException;
    public void STORE(List<Comment> data) throws RemoteException;
    public void STORE(byte[] data) throws RemoteException;
    public void START() throws RemoteException;
    public void SET() throws RemoteException;
    public int STORE_SIZE() throws RemoteException;
    public int TASK_SIZE() throws RemoteException;
    public PublicKey GET_KEY() throws RemoteException;
    public void SET_SYM_KEY(byte[] symKey, CommandServerProtocol commandServer) throws RemoteException;

}
