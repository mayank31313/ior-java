package ai.mayank.iot;

import java.util.HashMap;
import java.util.Random;

public class Client{
    static  IOTClient client1;
    static IOTClient client2;
    static long time;
    final static String token = "c7024ca7-57a2-4c89-978c-121fb8152312";

    public static void main(String [] args) throws Exception {
        client1 = new IOTClient(555,1234,token,true);
        client1.setReadFunction(Client::readFunction1);
        client1.setSync(Client::sync);

        while(true){
            Thread.sleep(1000);
            time = System.currentTimeMillis();
            System.out.println("Sending Message");
            client1.sendMessage("Messaage from client 1");
        }
    }
    public static HashMap<String,String> sync(Integer datas){
        HashMap<String,String> map = new HashMap<>();
        Random random = new Random();
        map.put("34",String.valueOf(random.nextInt(255)));
        map.put("44",String.valueOf(random.nextInt(255)));
        return map;
    }
    public static String readFunction1(SocketMessage msg){
        System.out.printf("Message Received from: %s\n",msg.from);
        System.out.println("Message Received: " + msg.message);
        System.out.println("Message status: " + msg.status);
        if(msg.status == Status.Operation){
            System.out.println("\tPort: " + msg.port);
            System.out.println("\tState: " + msg.state);
        }
        return "<ACKNOWLEDGED>";
    }
    public static String readFunction2(SocketMessage msg){
        System.out.println();
        System.out.println("Message Received in: " + String.valueOf(System.currentTimeMillis() - time) + " milli seconds, Message: "+msg.message);
        time = System.currentTimeMillis();
        return "<ACKNOWLEDGED>";
    }
}

