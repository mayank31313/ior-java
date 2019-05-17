package ai.mayank.iot.examples;

import ior_research.iotclient.*;

public class Client{
    static IOTClient client1;
    static long time;
    final static String token = "826f7556-6442-4c09-9e1e-76dbb462542c";
    final static Integer from = 555,to=789;

    public static void main(String [] args) throws Exception {
        client1 = new IOTClient(from,to,token,true);
        client1.setReadFunction(Client::readFunction1);

        while(true){
            Thread.sleep(1000);
            time = System.currentTimeMillis();
            System.out.println("Sending Message");
            client1.sendMessage("Messaage from client 1");
        }
    }
    public static String readFunction1(SocketMessage msg) {
        System.out.println("Message Received");
        System.out.println("Message Received: " + msg.message);
        System.out.println("Message status: " + msg.status);
        return "<ACKNOWLEDGED>";
    }
}

