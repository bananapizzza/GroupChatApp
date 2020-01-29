import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Scanner;

public class GroupChat {
    private static final String TERMINATE = "Exit";
    static String name;
    static volatile boolean finished = false;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Two arguments required: <multicast-host> <port-number>");
        } else {
            try {
                InetAddress group = InetAddress.getByName(args[0]);
                int port = Integer.parseInt(args[1]);
                Scanner sc = new Scanner(System.in);
                System.out.println("Enter your name: ");
                name = sc.nextLine();
                MulticastSocket socket = new MulticastSocket(port);

                socket.setTimeToLive(0);

                socket.joinGroup(group);
                Thread t = new Thread(new ReadThread(socket, group, port));
                t.start();

                System.out.println("Start typing messages...");
                while (true) {
                    String message;
                    message = sc.nextLine();
                    if (message.equalsIgnoreCase(GroupChat.TERMINATE)) {
                        finished = true;
                        socket.leaveGroup(group);
                        socket.close();
                        break;
                    }
                    message = name + ": " + message;
                    byte[] buffer = message.getBytes();
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, group, port);
                    socket.send(datagramPacket);
                    System.out.println("Data sent");
                }
            } catch (SocketException e) {
                System.out.println("Error creating socket");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Error reading/writing from/to socket");
                e.printStackTrace();
            }
        }
    }
}

class ReadThread implements Runnable {
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    private static final int MAX_LEN = 1000;

    ReadThread(MulticastSocket socket, InetAddress group, int port) {
        this.socket = socket;
        this.group = group;
        this.port = port;
    }

    public void run() {
        while (!GroupChat.finished) {
            byte[] buffer = new byte[ReadThread.MAX_LEN];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, group, port);
            String message;
            try {
                socket.receive(datagramPacket);
                System.out.println("data received");
                message = new String(buffer, 0, datagramPacket.getLength(), "UTF-8");
                if (!message.startsWith(GroupChat.name)) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Socket closed!");
            }
        }
    }
}
