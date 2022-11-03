import java.io.*;
import java.net.*;

public class Main {
    public final static int SERVER_PORT = 99;

    public static void main(String[] ar) {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                System.out.println("연결 대기중...");
                Socket s = ss.accept();
                new ServerThread(s).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}