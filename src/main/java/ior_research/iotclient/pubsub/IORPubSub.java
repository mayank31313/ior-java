package ior_research.iotclient.pubsub;

import ior_research.iotclient.IOTClient;
import ior_research.iotclient.SocketMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.auth.AuthenticationToken;
import org.apache.pulsar.common.policies.data.AuthAction;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IORPubSub extends IOTClient {
    Logger log = Logger.getLogger(IORPubSub.class.getName());

    public IORPubSub(Integer from,Integer to,String token,boolean log) throws IOException {
        super(from,to,token,log);
    }
    public IORPubSub(Integer from,Integer to,String token,boolean log,String serverName) throws  IOException{
        super(from,to,token,log);
    }
    private Producer<byte[]> producer;
    private Consumer consumer;
    private PulsarClient pulsar_client;

    public static List<IOTClient> createRevertedClients(int from, int to, String token, boolean log, String server) throws IOException{
        IORPubSub c1 = new IORPubSub(from,to,token,log,server);
        IORPubSub c2 = new IORPubSub(to,from,token,log,server);
        return Arrays.asList(new IORPubSub[]{c1,c2});
    }

    @Override
    public void send(SocketMessage msg) throws IOException{
        String s = gson.toJson(msg);
        producer.send(s.getBytes());
    }

    @Override
    protected boolean reconnect() throws IOException{
        String server = String.format("http://%s:8080/IOT/dashboard/socket/subscribe/%s/%d/%d",serverName,token,from,to);
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost postRequest = new HttpPost(server);
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

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String read = reader.readLine();
        reader.close();
        print(read);

        pulsar_client = PulsarClient.builder()
                .serviceUrl("pulsar://192.168.0.102:6650")
                .authentication(AuthenticationFactory.token("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.Yin2_oIj-lZMcoX-mxQXmG3yzFjSGqSsx61_cjtP51Q"))
                .build();

        producer = pulsar_client.newProducer()
                .topic(this.token + "-" + String.valueOf(from))
                .create();

        consumer = pulsar_client.newConsumer()
                .topic(this.token + "-" + String.valueOf(to))
                .subscriptionName("ior-consume-" + read)
                .subscribe();

        return true;
    }

    @Override
    public void run(){
        while (!this.isInterrupted()) {
            try {
                // Wait for a message
                Message msg = consumer.receive();
                try {
                    consumer.acknowledge(msg);
                    if(readFunction != null) {
                        SocketMessage s = gson.fromJson(new String(msg.getData()),SocketMessage.class);
                        readFunction.apply(s);
                    }

                } catch (Exception e) {
                    consumer.negativeAcknowledge(msg);
                }
            }catch(Exception ex) {
                if(ex.getCause().getClass() == InterruptedException.class){
                    break;
                }else{
                    ex.printStackTrace();
                }
            }
        }
        System.out.println("Thread Terminated");
    }

    @Override
    public  void close(){
        this.interrupt();
        try {
            this.producer.close();
        }catch(PulsarClientException ex){log.log(Level.WARNING,"Producer Not Closed");}
        try {
            this.consumer.close();
        }catch(PulsarClientException ex){log.log(Level.WARNING,"Consumer Not Closed");}
        try {
            this.pulsar_client.close();
        }catch(PulsarClientException ex){log.log(Level.WARNING,"Client Not Closed");}
    }
}
