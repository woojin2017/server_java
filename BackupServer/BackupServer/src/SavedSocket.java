import java.net.Socket;
import java.util.HashMap;

public class SavedSocket {
    public static Socket doorLocks = null;
    public static HashMap<String, Socket> clients = new HashMap<>();
}
