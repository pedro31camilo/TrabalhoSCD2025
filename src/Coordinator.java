import java.net.*;

public class Coordinator {

    public static void main(String[] args) {
        while(true) {

        }
    }

    private static void threadAtendimento() {
        try {
            DatagramSocket serviceSocket = new DatagramSocket(5000);

            byte[] receivedBytes = new byte[10];
            DatagramPacket receivedPacket =
                    new DatagramPacket(receivedBytes, receivedBytes.length);

            serviceSocket.receive(receivedPacket);
            String message = new String(receivedPacket.getData());
            System.out.println(message);

            // InetAddress ipCliente = receivedPacket.getAddress();
            // int portaCliente = receivedPacket.getPort();

            new Thread(() -> {
                threadTratamento(message, receivedPacket);
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private static void threadTratamento(String texto, DatagramPacket pacote) {
        /*
        if m.request
            if Q.empty
             send(m.process,Grant)
             Q.add(m.process)
         if m.release
            Q.remove()
            if !Q.empty
             process = Q.head()
             send(process,Grant)
        * */
    }
}
