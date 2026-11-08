package utb.fai;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.io.IOException;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {

            InetAddress serverAddress = InetAddress.getByName("localhost");
            final int port = 9999;
            final byte[] buffer = new byte[1024];

            while (true) {
                System.out.println("Zadejte zpravu:");
                String message = scanner.nextLine();

                byte[] data = message.getBytes();

                DatagramPacket packet = new DatagramPacket(
                    data, data.length, serverAddress, port
                );

                socket.send(packet);

                DatagramPacket response = new DatagramPacket(
                    buffer, buffer.length
                );

                socket.receive(response);

                String responseString = new String(
                    response.getData(),
                    0,
                    response.getLength()
                );

                System.out.println(responseString);
            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
