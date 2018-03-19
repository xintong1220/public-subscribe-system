package pssystem;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client extends Socket{
    private static final String IP_ADDRESS = "127.0.0.1";
    private String myIp;
    private int port;
    private BufferedReader in;
    private List<Integer> topics;

    private DatagramSocket server;
    
    /**
     * connect to server, and read message from stdin
     */
    public Client(int port) throws Exception {
//        super(IP_ADDRESS, port);
        this.port = port;
        server = new DatagramSocket(port);
        in = new BufferedReader(new InputStreamReader(System.in));
        myIp = IP_ADDRESS + ":" + port;
    }
    private void start() {
        ExecutorService executor = Executors.newCachedThreadPool();
        //  The receiver thread is a passive player that handles
        //  merging incoming membership lists from other neighbors.
        executor.execute(new Client.readLineThread());
        executor.execute(new Client.ReadFromSTD());
    }
    
    /**
     * listen to the server
     */
    class readLineThread extends Thread {
        @Override
        public void run() {
            try {
                while(true) {
                    byte[] buf = new byte[256];
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    server.receive(p);

                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    Object readObject = ois.readObject();

                    String msg = (String) readObject;

                    if(msg.equals("ok")) {
                        System.out.println("Subscription success.");
                    } else if(msg.equals("exit")) {
                        break;
                    } else {
                        System.out.println("Received message: " + msg);
                    }
                }
                
                in.close();
            } catch (Exception e) {
                System.out.println("Exception:" + e);
            }
        }
    }

    class ReadFromSTD extends Thread {
        @Override
        public void run() {
            try {
                while(true) {
                    String input = in.readLine();

                    if (input != null && input.length() > 0) {
                        System.out.println("read input from cmd: " + input);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(myIp + " " + input);
                        byte[] buffer = baos.toByteArray();

                        DatagramSocket socket = new DatagramSocket();
                        for(int port = 2222; port <= 2223; ++port) {
                            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(IP_ADDRESS), port);
                            socket.send(datagramPacket);
                        }
                        socket.close();
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception:" + e);
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            new Client(Integer.valueOf(args[0])).start(); // start the client, arguments: the port number
        } catch (Exception e) {
            System.out.println("Exception:" + e);
        }
    }
}
