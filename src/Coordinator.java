import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashMap;
import java.util.Scanner;

public class Coordinator {

    private static final Queue<Integer> Q = new LinkedList<>();
    private static final String LOG_FILE = "coordinator_log.txt";
    private static HashMap<Integer, Integer> contadorDeProcessos = new HashMap<>();

    public static void main(String[] args) {
        iniciarThreadRecepcao();
        iniciarThreadInterface();
    }

    private static void iniciarThreadRecepcao() {

        new Thread(() -> {
            threadAtendimento();
        }).start();
    }

    private static void threadAtendimento() {
        try {
            DatagramSocket socket = new DatagramSocket(5000);
            System.out.println("Coordenador ouvindo na porta 5000...");

            while (true) {
                byte[] buf = new byte[Message.TAMANHO_MENSAGEM];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.receive(packet);
                String texto = new String(packet.getData()).trim();
                Message msg = Message.transformaString(texto);

                writeLog(msg);

                new Thread(() -> threadProcessamento(msg, packet, socket)).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void threadProcessamento(Message m, DatagramPacket pacote, DatagramSocket socket) {
        synchronized (Q) {
            if (m.getType() == m.REQUEST) {
                if (Q.isEmpty()) {
                    enviarGrant(m.getProcessId(), pacote, socket);
                }

                Q.add(m.getProcessId());
            }

            else if (m.getType() == m.RELEASE) {
                Q.remove();

                if (!Q.isEmpty()) {
                    enviarGrant(Q.peek(), pacote, socket);
                }
            }

        }
    }

    private static void enviarGrant(int processId, DatagramPacket pacote, DatagramSocket socket) {
        try {
            Message grant = new Message(2, processId);
            byte[] data = grant.transformaMessage().getBytes();

            DatagramPacket resp = new DatagramPacket(
                    data,
                    data.length,
                    pacote.getAddress(),
                    pacote.getPort()
            );

            socket.send(resp);

            incrementarContador(grant.getProcessId());
            writeLog(grant);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized void incrementarContador(int id) {
        contadorDeProcessos.put(id, contadorDeProcessos.getOrDefault(id, 0) + 1);
    }

    private static void writeLog(Message m) {
        synchronized (FileHelper.class) {

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS"));

            String tipo = switch (m.getType()) {
                case 1 -> "REQUEST";
                case 2 -> "GRANT";
                case 3 -> "RELEASE";
                default -> "UNKNOWN";
            };

            String origemOuDestino = (tipo.equals("GRANT")) ?
                    "Destino=" + m.getProcessId() :
                    "Origem=" + m.getProcessId();

            String linha = String.format("%s | %s | %s", timestamp, tipo, origemOuDestino);

            FileHelper.appendToFile(linha, LOG_FILE);
        }
    }

    private static void iniciarThreadInterface() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("\nComandos disponíveis:");
                System.out.println("1) fila");
                System.out.println("2) contadores");
                System.out.println("3) sair");
                System.out.print("> ");

                int cmd = scanner.nextInt();

                switch (cmd) {
                    case 1:
                        imprimirFila();
                        break;

                    case 2:
                        imprimirContadores();
                        break;

                    case 3:
                        System.out.println("Encerrando coordenador...");
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Comando não reconhecido.");
                }
            }
        }).start();
    }

    private static void imprimirFila() {
        synchronized (Q) {
            if (Q.isEmpty()) {
                System.out.println("Fila vazia.");
            } else {
                System.out.println("Fila atual: " + Q);
            }
        }
    }

    private static void imprimirContadores() {
        synchronized (Coordinator.class) {
            if (contadorDeProcessos.isEmpty()) {
                System.out.println("Nenhum processo foi atendido ainda.");
            } else {
                System.out.println("Quantidade de grants por processo:");
                contadorDeProcessos.forEach((id, qtd) -> {
                    System.out.println("Processo " + id + " -> " + qtd + " grants");
                });
            }
        }
    }
}
