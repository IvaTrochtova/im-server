package utb.fai;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;

public class Server {
    public static void main(String[] args) throws IOException {
        try(DatagramSocket socket = new DatagramSocket(9999)) {
            final byte[] buffer = new byte[1024];
            while(true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Byla prijata zprava od klienta [" + packet.getAddress() + "/" + packet.getPort() + "] : " + message);

                String echoMessage = "SERVER: " + message;
                byte[] response = echoMessage.getBytes();
                DatagramPacket echo = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
                socket.send(echo);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }  
}