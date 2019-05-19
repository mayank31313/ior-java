package ior_research.iotclient;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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
    final String serverName = "iorresearch.ml";

    private BufferedReader reader = null;
    private BufferedWriter writer = null;

    private long time;
    private final int delay = 1000 * 60;
    private boolean log;
    private Function<SocketMessage,Boolean> readFunction;

    /**
     * Constructor for IOTClient, initialize all things that will be required in future.
     * @param from Current Device code
     * @param to Destination Device code
     * @param token Subscription key, you can find it in settings page
     * @param log choose weather to log the values in CLI Screen, boolean typr.
     * @throws IOException
     */
    public IOTClient(Integer from,Integer to,String token,boolean log) throws IOException {
        this.log = log;
        this.from = from;
        this.to = to;
        this.token = token;

        if(!reconnect())
            throw new IOException("Could not connect to Server");
    }

    /**
     * Connects the client to the server
     * @return
     * @throws IOException
     */
    private boolean reconnect() throws IOException{
        String server = String.format("http://%s/IOT/dashboard/socket/subscribe/%s/%d/%d",serverName,token,from,to);
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet getRequest = new HttpGet(server);
        HttpResponse response = httpClient.execute(getRequest);
        int status = response.getStatusLine().getStatusCode();
        while(status == 404){
            response = httpClient.execute(getRequest);
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
        time = System.currentTimeMillis();
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
            time = System.currentTimeMillis();
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
                if(System.currentTimeMillis() - time > delay){
                        sendHeartBeat();
                }
            }
            catch(SocketException ex){
                print("Reconnecting...");
                try {
                    client.close();
                    reconnect();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }


    /**
     * Read data from the server, if exists
     * @return SocketMessage if received else null
     * @throws IOException
     */
    public SocketMessage readData()throws IOException{
        if (client.getInputStream().available() == 0) return null;
        String dataString = reader.readLine();
        SocketMessage msg = gson.fromJson(dataString,SocketMessage.class);
        return msg;
    }

}