import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

class ServerThread extends Thread {
    private Socket s;
    private BufferedReader br;
    static File img = new File("a.png");

    public ServerThread(Socket s) {
        this.s = s;
        try {
            this.s.setTcpNoDelay(true);
            br = new BufferedReader(new InputStreamReader(s.getInputStream()));    //Socket으로 Read용 Stream
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            String[] received;
            try {
                received = br.readLine().split(" ");

                if(received == null) {
                    System.out.println("null");
                    return;
                } else if(received[0].equals("doorlock")) {
                    if(received[1].equals("append")) {
                        SavedSocket.doorLocks = this.s;
                        //System.out.println("append to doorlocks");
                    } else if(received[1].equals("pw")) {
                        // 기간제 암호와 비교하여 판별 결과 전송
                        // 형식 : doorlock pw password
                        //System.out.println(received[2]);
                        if(SavedData.globalPassword.contains(received[2])) {
                            SavedSocket.doorLocks.getOutputStream().write("o".getBytes());
                            SavedSocket.doorLocks.getOutputStream().flush();
                            Date date_now = new Date(System.currentTimeMillis());
                            SimpleDateFormat fourteen_format = new SimpleDateFormat("yyyy/MM/dd-HH:mm");
                            sendAllClient(fourteen_format.format(date_now) +" Success\n");
                        } else {
                            System.out.println("failed");
                            Date date_now = new Date(System.currentTimeMillis());
                            SimpleDateFormat fourteen_format = new SimpleDateFormat("yyyy/MM/dd-HH:mm");
                            sendAllClient(fourteen_format.format(date_now) +" Fail\n");
                        }
                    } else if(received[1].equals("log")) {
                        // 전송받은 로그 내부에 저장 && 클라이언트에 알림 전송
                        // 형식 : doorlock log time state
                        SavedData.logs.add(received[2] +" " +received[3]);
                        sendAllClient(received[2] +" " +received[3] +"\n");
                    }
                } else if(received[0].equals("client")) {
                    String user = received[1];

                    if(received[2].equals("append")) {
                        SavedSocket.clients.put(user, this.s);
                        this.s.getOutputStream().write("ACK\n".getBytes());
                        this.s.getOutputStream().flush();
                        //System.out.println("append to clients");
                    } else if(received[2].equals("open")) {
                        System.out.println("open the door!");
                        Date date_now = new Date(System.currentTimeMillis());
                        SimpleDateFormat fourteen_format = new SimpleDateFormat("yyyy/MM/dd-HH:mm");
                        sendAllClient(fourteen_format.format(date_now) +" Success\n");

                        // 도어락에 데이터 전송
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    SavedSocket.doorLocks.getOutputStream().write("o".getBytes());
                                    SavedSocket.doorLocks.getOutputStream().flush();
                                } catch (Exception e) {}
                            }
                        }).start();
                    } else if(received[2].equals("mkpw")) {
                        // 기간제 암호 생성
                        // 형식 : client user mkpw password
                        //if(!SavedData.password.containsKey(user)) SavedData.password.put(user, new HashSet<>());
                        //SavedData.password.get(user).add(received[3]);
                        SavedData.globalPassword.add(received[3]);
                        System.out.println("add PW " +received[3]);
                    } else if(received[2].equals("rmpw")) {
                        // 기간제 암호 삭제
                        // 형식 : client user rmpw password
                        System.out.println("rm PW " +received[3]);
                        if(SavedData.globalPassword.contains(received[3])) SavedData.globalPassword.remove(received[3]);
                    } else if(received[2].equals("getlog")) {
                        // 저장된 로그 클라이언트로 전송
                        // 형식 : client user getlog time1 time2
                    } else if (received[2].equals("getcam")) {
                        // 카메라 데이터 받아오기
                        SavedSocket.imageRecv = false;
                        SavedSocket.camera.getOutputStream().write("capture".getBytes());
                        while(!SavedSocket.imageRecv) sleep(100);

                        System.out.println("Image to client");
                        FileInputStream fis = new FileInputStream(img);
                        OutputStream os = this.s.getOutputStream();
                        byte buffer[] = new byte[4096];

                        int size = -1;
                        while ((size = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, size);
                            os.flush();
                        }
                        fis.close();
                        os.close();

                        SavedSocket.imageRecv = false;
                        System.out.println("Image Sended!!!");
                    }
                } else if (received[0].equals("camera")) {
                    if (received[1].equals("append")) {
                        SavedSocket.camera = this.s;
                    } else if (received[1].equals("image")) {
                        System.out.println("image recv");

                        FileOutputStream fos = new FileOutputStream(img);

                        InputStream is = this.s.getInputStream();
                        byte buffer[] = new byte[50000];

                        int size = -1;
                        while ((size = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, size);
                            fos.flush();
                        }

                        fos.close();
                        is.close();

                        SavedSocket.imageRecv = true;
                        System.out.println("image received!!!");
                        return;
                    }
                } else {
                    System.out.println(received[0]);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                System.out.println(this.s.getInetAddress());
                e.printStackTrace();
                return;
            }
        }
    }

    public static void sendAllClient(String s) {
        System.out.println(s);
        for(Socket sc : SavedSocket.clients.values()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(sc.getOutputStream()));
                        bw.write(s.strip() +"\n");
                        bw.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}