package solution;

import data.models.*;
import algorithms.*;
import javafx.util.Pair;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.util.*;

public class Client implements ClientProtocol {


    private VolenteerRegistryServerProtocol server;
    // Client variables
    boolean idle;

    //Storage variables all in #comments
    private int memorySize;             // memory size the client holds, does not mean it's all made available
    private int availableSize;
    private int storeSize;              // the size that is made available for storage or is still available
    private int size;                   // max size that client can manage at once
    PrivateKey priv;
    PublicKey pub;
    SecretKey symKey;
    private CommandServerProtocol commandServer;


    //Storage
    ArrayList<Comment> memory;
    Queue<Integer> servingQueue;      // keep the size of each batch


    public Client(int size, int memorySize, int storeSize) throws RemoteException {

        idle = true;       //every client is idle when connecting.

        // Makes sure that the sizes are correct
        if (storeSize > memorySize && size < storeSize) {
            System.err.println("availableMemory is larger than memorySize: " + "available: " + storeSize + "memory size: " + memorySize);
        } else {
            this.storeSize = storeSize;
        }

        this.memorySize = memorySize;
        this.storeSize = storeSize;
        this.availableSize = storeSize;
        this.size = size;

        memory = new ArrayList<>();
        servingQueue = new LinkedList<>();     // bijhouden hoeveel iedere batch groot is. om juiste hoeveelheid data weg te kunnen smijten

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keyGen.initialize(1024, random);
            KeyPair pair = keyGen.generateKeyPair();
            priv = pair.getPrivate();
            pub = pair.getPublic();
            symKey = null;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Client keyGen error" + e);
        } catch (NoSuchProviderException e) {
            System.out.println("Client keyGen error" + e);
        }


    }

        /*
    Get the client's public key
     */

    public PublicKey GET_KEY() {
        return pub;
    }

    public void SET_SYM_KEY(byte[] symKey, CommandServerProtocol commandServer) {
        System.out.println("Set Sym Key");
        Cipher cipher;
        SecretKey secretKey;
        this.commandServer = commandServer;

        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, priv);

            byte[] recoveredBytes = cipher.doFinal(symKey);
            secretKey = new SecretKeySpec(recoveredBytes, "AES");

            this.symKey = secretKey;
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            System.out.println("Set sym key error " + e);
        }

    }

    public static byte[] convertToByteArray(float value) {
        byte[] bytes = new byte[4];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putFloat(value);
        return buffer.array();

    }


    private byte[] encrypt(float num) {
        Cipher cipher;
        byte[] encryptedMessage = null;

        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, symKey);
            byte[] message = convertToByteArray(num); //-> hierachter de hash plakken?

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            sha256_HMAC.init(symKey);

            byte[] hash = sha256_HMAC.doFinal(message);
            Pair<Float,byte[]> send = new Pair<>(num, hash);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(send);

            encryptedMessage = cipher.doFinal(out.toByteArray());

            return encryptedMessage;

        } catch (IOException| InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            System.out.println("Decryption error client" + e);
        }

        return encryptedMessage;
    }


    private List<Comment> decrypt(byte[] data) {
        Cipher cipher;
        List<Comment> comments = new ArrayList<>();

        try {
            System.out.println("try");
            cipher = Cipher.getInstance("AES");
            System.out.println("cipher");
            cipher.init(Cipher.DECRYPT_MODE, symKey);
            System.out.println("init");


            byte[] recoveredBytes = cipher.doFinal(data);
            ByteArrayInputStream in = new ByteArrayInputStream(recoveredBytes);
            ObjectInputStream is = new ObjectInputStream(in);
            Pair<List<Comment>,byte[] > message = (Pair<List<Comment>,byte[] >) is.readObject();        // deserialize the object and cast to the correct type

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            sha256_HMAC.init(symKey);


            ByteArrayOutputStream outs = new ByteArrayOutputStream();    //serialize the data before hashing
            ObjectOutputStream oss = new ObjectOutputStream(outs);
            oss.writeObject(message.getKey());

            byte[] hash = sha256_HMAC.doFinal(outs.toByteArray());        // hash the data, it is used as signature by client

            if(Arrays.equals(hash, message.getValue())){                // check signatures
                System.out.println("Decryption " + message.getKey());
                return message.getKey();

            }

        } catch (IOException | ClassNotFoundException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            System.out.println("Decryption error client" + e);
        }

        return comments;
    }


    public int SIZE() {
        return size;
    }

    public int TASK_SIZE() {
        return servingQueue.size();
    }

    public int STORE_SIZE() {
        return storeSize;
    }

    /*
    Store given data into the clients memory
    store is used as a helper function by STORE()
    STORE() makes sure that data larger than size is split up in smaller pieces and corresponding size is provided in the queue
     */

    private void store(List<Comment> comments) {         // helper function, used to store smaller partitions into memory: used in STORE()
        for (Comment comment : comments) {
            memory.add(comment);
            storeSize--;
        }

        servingQueue.offer(comments.size());
    }

    /*
    Store non encrypted data
     */
    public void STORE(List<Comment> comments) {

        if (comments.size() <= size && comments.size() <= storeSize) {
            store(comments);
        } else {
            System.out.println("Request can't be handled not enough memory available: Available memory " + storeSize);
            System.out.println("Request is too large " + size);
        }

    }

    /*
    Store encrypted data
     */
    public void STORE(byte[] data) {

        List<Comment> comments = decrypt(data);

        STORE(comments);

    }

    /*
    Switch the client's state
     */

    public void SET() {

        idle = !idle;
        System.out.println("New State: " + idle);

    }

    /*
    Start the algorithm for calculation + memory management after the calculation
     */
    public void START() throws RemoteException {

        System.out.println("Command received");
        ArrayList<Comment> workingData = new ArrayList<>();
        while (servingQueue.size() > 0) {

            int datasize = servingQueue.poll();

            for (int i = 0; i < memory.size() && i <= datasize; i++) {
                workingData.add(memory.get(i));             // copy the data in the working array for the algorithm
            }

            float result = SequentialAnalyser.NonFilteredRun(workingData);
            delete_data(datasize);                              // remove the data from memory and from the queue
            System.out.println("delete " +datasize);
            System.out.println("delete " + storeSize);
            commandServer.RESULT(encrypt(result),this); // call the RESULT method of the command server who gave us this task
            // return
        }
        symKey = null;                              // reset the symmetric key after finishing the calculation
    }

    /*
    Remove no longer needed data, and it's size from the queue, used in START()
    */
    private void delete_data(int dataSize) {

        System.out.println("datasize: " + dataSize);
        System.out.println("storeSize: "+ storeSize);
        while (dataSize > 0) {
            memory.remove(0);
            dataSize--;
            storeSize++;
        }

    }


    /*
    Connect to the volenteer registry server
     */
    private void connect() {

        try {
            Registry reg = LocateRegistry.getRegistry();
            server = (VolenteerRegistryServerProtocol) reg.lookup("VolenteerRegisteryServer");
            ClientProtocol stub = (ClientProtocol) UnicastRemoteObject.exportObject(this, 0);
            server.ADD(stub);

        } catch (RemoteException e) {
            System.out.println("Client remote exception: " + e.getMessage());
        } catch (NotBoundException e) {
            System.out.println("Client not bound exception: " + e.getMessage());
        }
    }

    /*
    Close the connection with the connected volenteer registry server
     */
    private void close() {
        try {
            symKey = null;              // remove the symmetric key when closing the connection
            server.REMOVE(this);

        } catch (RemoteException e) {
            System.out.println("Client remote exception: " + e.getMessage());
        }
    }


    public static void main(String[] args) throws RemoteException {
        Client test = new Client(10, 10, 10);
        test.connect();
    }

}

