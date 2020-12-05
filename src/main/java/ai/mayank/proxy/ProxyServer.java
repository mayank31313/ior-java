package ai.mayank.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ior_research.iotclient.IOTClient;
import ior_research.iotclient.SocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ProxyServer{
    private Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    private ServerSocket serverSocket;
    private ProxyHandler handler = null;
    private ObjectMapper mapper;
    private IOTClient iotClient;
    public ProxyServer(IOTClient client) throws IOException {
        this.iotClient = client;
        this.mapper = new ObjectMapper();
        this.serverSocket = new ServerSocket(5000);
        this.iotClient.setReadFunction(this::onReceive);
    }
    public void start(){
        while(true){
            try {
                Socket socket = serverSocket.accept();
                handler = new ProxyHandler(socket, this::onSend);
                handler.start();
                handler.join();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                handler = null;
            }
        }
    }
    private boolean onReceive(SocketMessage message) {
        if(handler == null){
            logger.warn("Skipping onReceive");
            return false;
        }
        try {
            handler.onReceive(mapper.writeValueAsString(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    private boolean onSend(String data) {
        try {
            iotClient.send(mapper.readValue(data, SocketMessage.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}
