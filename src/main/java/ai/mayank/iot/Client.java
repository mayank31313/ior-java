package ai.mayank.iot;

import java.util.HashMap;
import java.util.Random;

public class Client{
    static  IOTClient client1;
    static IOTClient client2;
    static long time;
    final static String token = "c7024ca7-57a2-4c89-978c-121fb8152312";
    public static void main(String [] args) throws Exception {
        /*
        Integer code = 1234,to = 0;
        String token = "4eafc05a-a049-4b1b-a989-5b431f8bdbc1";
        //String token = "c012440f-b03d-4683-b533-15282165f2d5";
        for(int i=0;i<args.length;i++){
            if(args[i].equals("-from")){
                code = Integer.parseInt(args[i+1]);
                i++;
            }
            else if(args[i].equals("-token")){
                token = args[i+1];
                i++;
            }
            else if(args[i].equals("-to")){
                to = Integer.parseInt(args[i+1]);
                i++;
            }
        }

        client = new IOTClient(code,to,token,true);
        */
        client1 = new IOTClient(555,1234,token,false);
        //client2 = new IOTClient(555,1234,token,false);
        client1.setReadFunction(Client::readFunction1);
        client1.setSync(Client::sync);
        //client2.setReadFunction(Client::readFunction2);
        //client2.setSync(Client::sync);

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

