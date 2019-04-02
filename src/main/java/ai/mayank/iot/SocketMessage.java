package ai.mayank.iot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SocketMessage{
    public Integer from;
    public List<Integer> to;
    public String message;
    public Status status;
    public boolean state;
    public String port;
    public String token;
    public HashMap<String,String> syncData;
}