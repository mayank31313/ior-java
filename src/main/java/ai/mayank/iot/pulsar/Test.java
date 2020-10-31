package ai.mayank.iot.pulsar;


import ior_research.iotclient.IOTClient;
import ior_research.iotclient.SocketMessage;
import ior_research.iotclient.pubsub.IORPubSub;

import java.util.List;

public class Test extends Thread{
    IORPubSub pub_sub_1 = null;
    IORPubSub pub_sub_2 = null;

    final static String token = "5a5a83c3-2588-42fb-84bd-fa3129a2ac45";
    final static Integer from = 1234,to=789;

    public Test() throws Exception{

        List<IOTClient> clients = IORPubSub.createRevertedClients(from,to,token,true,"localhost");
        pub_sub_1 = (IORPubSub)clients.get(0);
        pub_sub_2 = (IORPubSub)clients.get(1);


        pub_sub_2.setReadFunction(this::onReceive);
        pub_sub_1.start();
        pub_sub_2.start();
    }

    public Boolean onReceive(SocketMessage msg) {
        System.out.println("Message Received");
        System.out.println("Message Received: " + msg.message);
        System.out.println("Message status: " + msg.status);
        return true;
    }

    public static void main(String[] args) throws Exception{
        Test t = new Test();
        t.start();
        SocketMessage msg = new SocketMessage();
        msg.message = "My Message";

        int i=0;
        while(i<10){
            t.pub_sub_1.send(msg);
            Thread.sleep(1000);
            i++;
        }

        t.pub_sub_1.close();
        t.pub_sub_2.close();
    }
}
