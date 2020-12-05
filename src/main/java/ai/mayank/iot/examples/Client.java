package ai.mayank.iot.examples;

import ior_research.iotclient.*;

import java.util.List;

public class Client{
    static IOTClient client1,client2;
    static long time;
    final static String token = "a9b08f66-8e6f-4558-b251-da7163aac420";
    final static Integer from = 1234,to=789;
    static float freq;
    public static void main(String [] args) throws Exception {
        List<IOTClient> clients = IOTClient.createRevertedClients(from,to,token,true,"localhost",5001,8000);


        client1 = clients.get(0);
        client1.setReadFunction(Client::onReceive);
        client1.start();

        client2 = clients.get(1);
        client2.setReadFunction(Client::onReceive);
        client2.start();

        Thread.sleep(250);
        long i=0;
        while(i<20){
            Thread.sleep(5000);
            time = System.currentTimeMillis();
            System.out.println("Sending Message");
            client1.sendMessage(String.valueOf(System.currentTimeMillis()));
            i++;
        }
        Thread.sleep(5000);

        client1.join();
        client2.join();

        System.out.println("Closed");
        System.exit(0);
    }

    public static Boolean onReceive(SocketMessage msg) {
        System.out.println("Message Received");
        System.out.println("Message Received: " + String.valueOf(System.currentTimeMillis() - Long.valueOf(msg.message)));
        System.out.println("Message status: " + msg.status);
        return true;
    }
}

