package ai.mayank.proxy;

import com.sun.org.apache.xpath.internal.operations.Bool;
import ior_research.iotclient.SocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.function.Function;

public class ProxyHandler extends Thread {
    private Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Function<String, Boolean> onSendFunction;

    public  ProxyHandler(Socket socket, Function<String, Boolean> onSend) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        this.onSendFunction = onSend;
    }
    public boolean onReceive(String data) throws IOException {
        this.writer.write(data);
        this.writer.newLine();
        this.writer.flush();
        return true;
    }
    @Override
    public void run(){
        while(true){
            try {
                String data = this.reader.readLine();
                onSendFunction.apply(data);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
