package solution;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class VolenteerRegistryServer extends UnicastRemoteObject implements VolenteerRegistryServerProtocol{


    private ArrayList<ClientProtocol> idleClients;
    private ArrayList<ClientProtocol> activeClients;
    private ArrayList<CommandServerProtocol> commandServers;


    public VolenteerRegistryServer() throws InterruptedException, RemoteException {

        // Create the lists to keep track of the clients
        idleClients = new ArrayList<>();
        activeClients = new ArrayList<>();
        commandServers = new ArrayList<>();

    }


    public void ADD(ClientProtocol client) {
            System.out.println("Client added: " + client);
            idleClients.add(client);
            System.out.println(idleClients.size());
    }

    public void ADD(CommandServerProtocol server) {
        System.out.println("Command server added: " + server);
        commandServers.add(server);
    }
    //TODO: verwijder een client wanneer deze het netwerk verlaat. aka verwijder deze uit de lijst

   public void REMOVE(ClientProtocol client){
        if(idleClients.contains(client)){
            System.out.println("Client removed: " + client);
            idleClients.remove(client);
        }else{
            System.out.println("Client is not idle, please try later");
        }

    }


    public void REMOVE(CommandServerProtocol server) {
        System.out.println("Client removed: " + server);
        idleClients.remove(server);
    }

    //TODO: get an address of an idle client
   public ClientProtocol GET(){
        if(idleClients.size() > 0){
            System.out.println("Client: " + idleClients.get(0) + "Retrieved");
            return idleClients.get(0);
        }
        else{
            return null;
        }
    }

    //TODO: Set the state of the client from idle to not idle
    public void SET(ClientProtocol client) throws RemoteException{             //actually changing the state of the client, must be performed by the CommandServer

        if(idleClients.contains(client)){
            activeClients.add(client);
            idleClients.remove(client);
        }else{
            idleClients.add(client);
            activeClients.remove(client);
        }
        System.out.println("Client switched state " + client);
    }


    private void makeAvailable() {
        try {

            LocateRegistry.getRegistry().rebind("VolenteerRegisteryServer", this);
        } catch(RemoteException e) {
            System.out.println("Server remote exception " + e.getMessage());
        }
    }

    public static void main(String[] args) throws RemoteException{
        try {

            new VolenteerRegistryServer().makeAvailable();
        }
        catch (InterruptedException io){
            System.out.println("Could not start up VolenteerRegisteryServer: " + io);
           io.printStackTrace();
        }

    }

    }




