import java.net.*;
import java.util.Scanner;

public class Processo {

    private static void conexaoProcesso() throws Exception {
        DatagramSocket processSocket = new DatagramSocket();

        System.out.println("Digite uma mensagem: ");
        Scanner teclado = new Scanner(System.in);
        String mensagem = teclado.nextLine();

        byte[] mensagemAEnviar = new byte[10];
        mensagemAEnviar = mensagem.getBytes();
        InetAddress ip = InetAddress.getByName("127.0.0.1");
        DatagramPacket packetAEnviar = new DatagramPacket(mensagemAEnviar, mensagemAEnviar.length, ip, 5000);
        processSocket.send(packetAEnviar);

        byte[] mensagemAReceber = new byte[10];
        DatagramPacket packetAReceber = new DatagramPacket(mensagemAReceber, mensagemAReceber.length);
        processSocket.receive(packetAReceber);
        String resposta = new String(packetAReceber.getData());
        System.out.println("Resposta do servidor: "+resposta);

        processSocket.close();
    }
}
