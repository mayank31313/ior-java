package ai.mayank.iot.examples;

import ior_research.iotclient.*;

public class Client{
    static IOTClient client1;
    static long time;
    final static String token = "ce84fbd8-1f83-4bc2-8754-e44f148718cd";
    final static Integer from = 1234,to=789;

    public static void main(String [] args) throws Exception {
        client1 = new IOTClient(from,to,token,true);
        client1.setReadFunction(Client::onReceive);
        client1.start();

        while(true){
            Thread.sleep(1000);
            time = System.currentTimeMillis();
            System.out.println("Sending Message");
            client1.sendMessage("Messaage from client 1");
        }
    }
    public static Boolean onReceive(SocketMessage msg) {
        System.out.println("Message Received");
        System.out.println("Message Received: " + msg.message);
        System.out.println("Message status: " + msg.status);
        return true;
    }
}

