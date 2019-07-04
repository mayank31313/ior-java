package ior_research.iotclient;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

import java.util.*;
import java.util.function.Function;

/**
 * Client Class for  IOR Server
 */

public class IOTClient extends Thread{
    public Socket client;
    Gson gson = new Gson();
    private Integer from,to;
    private String token;
    private final String serverName;
    private boolean isTunneled=false;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;

    private final Integer delay = 1000 * 90;
    private boolean log;
    private Function<SocketMessage,Boolean> readFunction;

    /**
     * Constructor for IOTClient, initialize all things that will be required in future.
     * @param from Current Device code
     * @param to Destination Device code
     * @param token Subscription key, you can find it in settings page
     * @param log choose weather to log the values in CLI Screen, boolean type.
     * @param serverName specify the server address default is iorresearch.ml
     * @throws IOException
     */
    public IOTClient(Integer from,Integer to,String token,boolean log,String serverName) throws IOException{
        this.log = log;
        this.from = from;
        this.to = to;
        this.token = token;
        this.serverName = serverName;

        if(!reconnect())
            throw new IOException("Could not connect to Server");
    }

    /**
     * IOT Client constructor
     * @param from specify the current device unique code
     * @param to specify the destination device unique code
     * @param token specify the subscription key
     * @param log weather the client should show the logs on CLI or now
     * @throws IOException
     */
    public IOTClient(Integer from,Integer to,String token,boolean log) throws IOException {
        this.log = log;
        this.from = from;
        this.to = to;
        this.token = token;
        this.serverName = "iorcloud.ml";

        if(!reconnect())
            throw new IOException("Could not connect to Server");
    }

    /**
     * Set the tunnel client
     * @param isTunneled true sets the IORClient in tunnel mode default false
     */
    public void setTunnel(boolean isTunneled){
        this.isTunneled = isTunneled;
    }

    /**
     * Get the version of the Client
     * @return Version
     */
    public static String version(){
        return "v0.3.7";
    }

    /**
     * Connects the client to the server
     * @return
     * @throws IOException
     */
    private boolean reconnect() throws IOException{
        String server = String.format("http://%s/IOT/dashboard/socket/subscribe/%s/%d/%d",serverName,token,from,to);
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost postRequest = new HttpPost(server);
        //HttpGet getRequest = new HttpGet(server);
        HttpResponse response = httpClient.execute(postRequest);
        int status = response.getStatusLine().getStatusCode();
        while(status == 404){
            response = httpClient.execute(postRequest);
            status = response.getStatusLine().getStatusCode();
            try {
                Thread.sleep(2 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            print(String.format("Server with address: %s not found",server));
        }

        if(status != 201)
            throw new IOException("Invalid Credentials");

        reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String read = reader.readLine();
        reader.close();
        print(read);

        print("Connecting to Server on port " + 8000);
        try {
            Thread.sleep(2 * 1000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        client = new Socket(serverName, 8000);
        print("Just connected to " + client.getRemoteSocketAddress());
        reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

        writer.write(read );
        writer.newLine();
        writer.flush();

        Thread heartBeat = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        sendMessage("<HEARTBEAT>");
                        Thread.sleep(delay);
                    }

                } catch (IOException|InterruptedException e) {
                    e.printStackTrace();
                }
                close();
            }
        });
        heartBeat.start();

        return true;
    }

    /**
     * Close the Input and Output Streams
     */
    public void close(){
        try {
            reader.close();
            writer.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send Data to server including Meta-Data
     * @param map a key-value pair Map, used to SYNC data from device to server. but it is now deprecated
     * @throws IOException
     */
    @Deprecated
    private void sendData(HashMap<String,String> map) throws IOException{
        SocketMessage msg = new SocketMessage();
        msg.status = Status.SYNC;
        msg.syncData = map;
        send(msg);
    }

    /**
     * Sends the Heart Beat to the server in a duration of time
     * @throws IOException
     */
    @Deprecated
    private void sendHeartBeat() throws IOException{
        SocketMessage msg = new SocketMessage();
        msg.message = "<HEARTBEAT>";
        send(msg);
    }

    /**
     * Send the socket message to the Server
     * @param msg Socket Message, specifies the Message to be end to the server.
     * @throws IOException
     */
    private void send(SocketMessage msg) throws IOException{
        String data = gson.toJson(msg);
        print(data);
        synchronized (writer) {
            writer.write(data);
            writer.newLine();
            writer.flush();
            print("Sending Message");
        }
    }


    private void print(String message){
        if(log)
            System.out.println(message);
    }

    /**
     * Sets the function that has to be called whenever client receives a Message from server.
     * @param f Function Object, can also be implemented using Lambda SocketMessage will be argument to function and Boolean is return type.
     */
    public void setReadFunction(Function<SocketMessage,Boolean> f){
        this.readFunction = f;
    }

    /**
     * Sends a string message to the server.
     * @param message holds the message value
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException{
        SocketMessage msg = new SocketMessage();
        msg.message = message;
        msg.status = Status.Operation;
        send(msg);
    }

    public void run() {
        while(true){
            try{
                SocketMessage msg = readData();
                if(msg!=null){
                    if(msg.message.equals("<RECOGNISED>")) {
                        print("Connection Stable");
                        continue;
                    }
                    if(msg.status == Status.Disconnected){
                        print("Received Disconnect Response from server");
                        this.close();
                        break;
                    }
                    if(readFunction != null) {
                        readFunction.apply(msg);
                        this.sendMessage("ack");
                    }
                }

            }catch(IOException e){
                e.printStackTrace();
                break;
            }
        }
        this.close();
    }


    /**
     * Read data from the server, if exists
     * @return SocketMessage if received else null
     * @throws IOException
     */
    public SocketMessage readData()throws IOException{
        String dataString = reader.readLine();
        SocketMessage msg = gson.fromJson(dataString,SocketMessage.class);
        return msg;
    }

}