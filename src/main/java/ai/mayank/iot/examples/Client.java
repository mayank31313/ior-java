package ai.mayank.iot.examples;

import ior_research.iotclient.*;

public class Client{
    static IOTClient client1;
    static long time;
    final static String token = "fc035650-886b-46bf-8fec-d9508d894e7a";
    final static Integer from = 555,to=789;

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

