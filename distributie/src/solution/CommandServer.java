package solution;

import java.io.*;
import java.nio.ByteBuffer;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;


import data.models.Comment;
import data.readers.RedditCommentLoader;
import javafx.util.Pair;

import javax.crypto.*;


class CommandServer implements CommandServerProtocol {

    private VolenteerRegistryServerProtocol server;

    private int maxTaskSize;
    private int dataIndex;              //keeps track of the already processed data
    private List<Comment> dataset;
    private ArrayList<ClientProtocol> busyClients;
    private ArrayList<Float> results;
    private Map<ClientProtocol, SecretKey> clientKeys;



    private SecretKey calcSymKey() {

        SecretKey symKey = null;
        try {
            symKey = KeyGenerator.getInstance("AES").generateKey();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("SymKey generation error " + e);
        }
        return symKey;
    }

    private void handShake(ClientProtocol client) throws RemoteException {

        PublicKey clientKey = client.GET_KEY();
        SecretKey symKey = calcSymKey();
        Cipher cipher;
        byte[] encryptedBytes;

        try {
            cipher = Cipher.getInstance("RSA");

            cipher.init(Cipher.ENCRYPT_MODE, clientKey);
            encryptedBytes = cipher.doFinal(symKey.getEncoded());

            client.SET_SYM_KEY(encryptedBytes, this);
            clientKeys.put(client, symKey);                  // when handshake completed, add to dictionary

        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            System.out.println("Handshake error " + e);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            System.out.println("cypher encrypt key error " + e);
        }
    }

    public static float convertToFloat(byte[] array) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        return buffer.getFloat();

    }

    public static byte[] convertToByteArray(float value) {
        byte[] bytes = new byte[4];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putFloat(value);
        return buffer.array();

    }


    private byte[] encrypt(List<Comment> data, ClientProtocol client) {
        Cipher cipher;
        byte[] encryptedMessage = null;
        SecretKey symKey = clientKeys.get(client);
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, symKey);

            ByteArrayOutputStream out = new ByteArrayOutputStream();    //serialize the data before encrypting
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(data);

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            sha256_HMAC.init(symKey);

            byte[] hash = sha256_HMAC.doFinal(out.toByteArray());
            Pair<List<Comment>,byte[]> send = new Pair<>(data, hash);

            ByteArrayOutputStream outs = new ByteArrayOutputStream();    //serialize the data before encrypting
            ObjectOutputStream oss = new ObjectOutputStream(outs);
            oss.writeObject(send);

            encryptedMessage = cipher.doFinal(outs.toByteArray());       // encrypt the byte array

            return encryptedMessage;

        } catch (IOException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            System.out.println("Decryption error client" + e);
        }

        return encryptedMessage;
    }

    private float decrypt(byte[] data, ClientProtocol client) {
        Cipher cipher;
        float result = 0;
        SecretKey symKey = clientKeys.get(client);

        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, symKey);

            byte[] recoveredBytes = cipher.doFinal(data);
            // result = convertToFloat(recoveredBytes);
            ByteArrayInputStream in = new ByteArrayInputStream(recoveredBytes);
            ObjectInputStream is = new ObjectInputStream(in);
            Pair<Float, byte[]> message = (Pair<Float, byte[]>) is.readObject();

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            sha256_HMAC.init(symKey);

            byte[] hash = sha256_HMAC.doFinal(convertToByteArray(message.getKey()));        // hash the data, it is used as signature by client


            if(Arrays.equals(hash, message.getValue())){
                return message.getKey();
            }
        } catch (IOException| ClassNotFoundException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            System.out.println("Decryption error client" + e);
        }

        return result;
    }


    //TODO: initiaize the path to the dataset
    public void SET(String path) {

        setDataset(path);

    }

    private void setDataset(String path) {

        try {
            // Multiple files can be provided. These will be read one after the other.
            String[] data = new String[]{
                    path,
            };

            dataset = RedditCommentLoader.readData(data);
        } catch (IOException e) {
            System.out.println(e.toString());
        }

    }

    private void sendData(ClientProtocol client) throws RemoteException {

        List<Comment> data;             //that is given to the clients as a parameter

        int size = client.SIZE();           // the max size of the request that the client can handle
        int storeSize = client.STORE_SIZE();    //available memory left in the client


        if (storeSize >= size && dataIndex + size <= dataset.size()) {       // you can fit a size request, and dataset is large enough
            data = new ArrayList<>(dataset.subList(dataIndex, dataIndex + size));
            dataIndex = dataIndex + size;

            client.STORE(encrypt(data, client));


        } else if (storeSize < size && dataIndex + storeSize < dataset.size()) {
            data = new ArrayList<>(dataset.subList(dataIndex, dataIndex + storeSize));
            dataIndex = dataIndex + storeSize;
            client.STORE(encrypt(data, client));

        }

    }

    /*
    Set up a new client for calculation.
     */
    private void setUpClient(ClientProtocol client) {

        try {

            server.SET(client);
            client.SET();
            busyClients.add(client);
            handShake(client);

        } catch (RemoteException e) {
            System.out.println(" Server remote exception " + e.getMessage());
        }
    }


    private ClientProtocol findNotToBusyClient() throws RemoteException {

        for (ClientProtocol client : busyClients) {
            if (client.TASK_SIZE() <= maxTaskSize && client.STORE_SIZE() > 0) {           // it has to be able to store something
                System.out.println("cient availalbe: " + client.STORE_SIZE());
                return client;
            }
        }
        return null;
    }

    private void updateClientState() throws RemoteException {
        System.out.println("# clients " + busyClients.size());
        if (busyClients.size() > 0) {
            for (ClientProtocol client : busyClients) {

                if (client.TASK_SIZE() == 0) {
                    System.out.println("client " + client);
                    client.SET();
                    server.SET(client);

                    clientKeys.remove(client);
                }

            }
            busyClients = new ArrayList<>(clientKeys.keySet());
        }
    }

    //TODO: Distribute the work over the available clients
    public void START() throws RemoteException {

        while (dataIndex < dataset.size() - 1) {

            updateClientState();                        // make sure all clients that are actually idle are set idle

            try {
                ClientProtocol client = server.GET();   // fetch new client for volenteerRegistry
                if (client != null) {

                    System.out.println(client);
                    setUpClient(client);                // if there is an idle client, set it up
                    sendData(client);
                    System.out.println(client);
                    client.START();

                } else if (busyClients.size() > 0) {

                    ClientProtocol busyClient = findNotToBusyClient();

                    if (busyClient != null) {           // if there is a client with a not to large queue, assign another task to it

                        sendData(busyClient);
                        System.out.println("data send not to busy " + dataIndex);
                    }

                }

            } catch (RemoteException e) {
                System.out.println(" Server remote exception " + e.getMessage());
            }
        }
    }


    //TODO: Combine all the results received from the clients

    public float RESULT( byte[] result, ClientProtocol client) throws RemoteException {
        System.out.println("result start");
        float newResult = decrypt(result, client);
        results.add(newResult);

        if (dataIndex == dataset.size() - 1 && busyClients.size() == 0) {

            float calcRes = 0;
            for (Float num : results) {
                calcRes += num;
                System.out.println(result);
            }
            updateClientState();                //clean up after the calculation
            return calcRes;
        } else {
            System.out.println("Please wait until all calculations are completed");
            return 0.0f;
        }
    }

    private void connect() {

        try {
            Registry reg = LocateRegistry.getRegistry();
            server = (VolenteerRegistryServerProtocol) reg.lookup("VolenteerRegisteryServer");
            CommandServerProtocol stub = (CommandServerProtocol) UnicastRemoteObject.exportObject(this, 0);
            server.ADD(stub);

        } catch (RemoteException e) {
            System.out.println("Command server remote exception: " + e.getMessage());
        } catch (NotBoundException e) {
            System.out.println("Command server not bound exception: " + e.getMessage());
        }
    }

    private void close() {
        try {

            server.REMOVE(this);

        } catch (RemoteException e) {
            System.out.println("Command Server remote exception: " + e.getMessage());
        }
    }


    public CommandServer(int maxTaskSize) {

        dataIndex = 0;
        this.maxTaskSize = maxTaskSize;
        busyClients = new ArrayList<>();
        results = new ArrayList<>();
        clientKeys = new HashMap<>();
    }


    public static void main(String[] args) {
        try {
            CommandServer server = new CommandServer(10);
            server.connect();
            server.setDataset("./src/files/dataset_1.json");
            System.out.println("START");
            server.START();
            System.out.println(server.dataIndex);

        } catch (Exception io) {
            System.out.println("CommandServer error: " + io);
            io.printStackTrace();
        }

    }
}
