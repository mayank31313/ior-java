package ai.mayank.iot;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.function.Function;

public class IOTClient extends Thread{
    public Socket client;
    Gson gson = new Gson();
    //JsonReader reader;
    HttpClient httpClient;
    Integer from;
    String token;
    Integer to;
    private static String server;
    final String serverName = "www.iorresearch.ml";

    private BufferedReader reader = null;
    private BufferedWriter writer = null;

    private long time;
    private final int delay = 1000 * 60;
    private boolean log;
    private Function<SocketMessage,String> readFunction;
    private Function<Integer,HashMap<String,String>> setSyncDataFunction;

    public IOTClient(Integer from,Integer to,String token,boolean log) throws IOException {
        this.log = log;
        this.from = from;
        this.to = to;
        this.token = token;
        httpClient = HttpClients.createDefault();
        server = String.format("http://%s/IOT/dashboard/socket/subscribe/%s/%s",serverName,token,from);
        if(!reconnect())
            throw new IOException("Could not connect to Server");

        this.start();
    }

    public void setSync(Function<Integer,HashMap<String,String>> f){
        this.setSyncDataFunction = f;
    }

    private boolean reconnect() throws IOException{
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
        /*
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder sb = new StringBuilder();
        String read = "";
        while((read = reader.readLine()) != null){
            sb.append(read);
        }
        read = sb.toString();
        print(read);
        List<DeviceElement> elements = gson.fromJson(read,ArrayList.class);
        if(elements == null)
            return false;
        */
        print("Connecting to Server on port " + 8000);
        try {
            Thread.sleep(2 * 1000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        client = new Socket(serverName, 8000);
        print("Just connected to " + client.getRemoteSocketAddress());
        time = System.currentTimeMillis();
        sendInitialMessage();
        return true;
    }

    private void sendInitialMessage() throws IOException{
        SocketMessage msg = new SocketMessage();
        msg.to = new ArrayList<>();
        msg.to.add(0000);
        msg.message = "<INITIALMESSAGE>";
        send(msg);
    }
    public void close(){
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendData(HashMap<String,String> map) throws IOException{
        SocketMessage msg = new SocketMessage();
        msg.status = Status.SYNC;
        msg.syncData = map;
        /*
        Random random = new Random();
        msg.syncData.put("34",String.valueOf(random.nextInt(255)));
        msg.syncData.put("44",String.valueOf(random.nextInt(255)));
        */
        send(msg);
    }
    private void sendHeartBeat() throws IOException{
        SocketMessage msg = new SocketMessage();
        msg.message = "<HEARTBEAT>";
        send(msg);
    }
    private void send(SocketMessage msg) throws IOException{
        OutputStream outToServer = client.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);
        print(gson.toJson(msg));
        out.writeBytes("<START>"+gson.toJson(msg)+"<END>");
        out.flush();
        time = System.currentTimeMillis();
        print("Sending Message");
    }
    private void print(String message){
        if(log)
            System.out.println(message);
    }
    void setReadFunction(Function<SocketMessage,String> f){
        this.readFunction = f;
    }

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
                    if(msg.message.equals("<RECOGNISED>") && msg.from.equals(0)) {
                        print("Connection Stable");
                        continue;
                    }
                    if(msg.status == Status.Disconnected && msg.from.equals(0)){
                        print("Received Disconnect Response from server");
                        this.close();
                        break;
                    }
                    if(readFunction != null)
                        readFunction.apply(msg);
                }
                if(System.currentTimeMillis() - time > delay){
                    if(this.setSyncDataFunction != null)
                        sendData(this.setSyncDataFunction.apply(0));
                    else
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

    public SocketMessage readData()throws IOException{
        DataInputStream in = new DataInputStream(client.getInputStream());
        if (in.available() == 0) return null;
        String dataString = "";
        int bytesRead = 0;
        long start = System.currentTimeMillis();
        while(!((dataString.contains("<START>") || dataString == "") && dataString.contains("<END>"))) {
            byte[] messageByte = new byte[100];
            while (in.available() > 0) {
                bytesRead = in.read(messageByte);
                dataString += new String(messageByte, 0, bytesRead);
                start = System.currentTimeMillis();
            }
            if(System.currentTimeMillis() - start > 2000)
                return null;
        }

        dataString = dataString.substring(7 + dataString.indexOf("<START>"),dataString.indexOf("<END>"));
        JsonReader reader = new JsonReader(new StringReader(dataString));
        reader.setLenient(true);
        SocketMessage msg = gson.fromJson(reader,SocketMessage.class);
        return msg;
    }
}