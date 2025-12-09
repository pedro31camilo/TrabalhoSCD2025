import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashMap;
import java.util.Scanner;

public class Coordinator {

    private static final Queue<Integer> Q = new LinkedList<>();
    private static final String LOG_FILE = "coordinator_log.txt";
    private static final HashMap<Integer, Integer> contadorDeProcessos = new HashMap<>();

    private static final HashMap<Integer, InetSocketAddress> enderecosProcessos = new HashMap<>();

    public static void main(String[] args) {
        iniciarThreadRecepcao();
        iniciarThreadInterface();
    }

    private static void iniciarThreadRecepcao() {
        new Thread(() -> {
            threadAtendimento();
        }, "Thread-Atendimento").start();
    }

    private static void threadAtendimento() {
        try {
            DatagramSocket socket = new DatagramSocket(5000);
            System.out.println("Coordenador ouvindo na porta 5000...");

            while (true) {
                byte[] buf = new byte[Message.TAMANHO_MENSAGEM];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                socket.receive(packet);
                String texto = new String(packet.getData(), 0, packet.getLength()).trim();
                Message msg = Message.transformaString(texto);

                if (msg != null) {
                    writeLog(msg);

                    new Thread(() -> threadProcessamento(msg, packet, socket),
                            "Thread-Processamento-" + msg.getProcessId()).start();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void threadProcessamento(Message m, DatagramPacket pacote, DatagramSocket socket) {
        synchronized (Q) {
            if (m.getType() == Message.REQUEST) {
                InetSocketAddress enderecoProcesso = new InetSocketAddress(
                        pacote.getAddress(),
                        pacote.getPort()
                );
                enderecosProcessos.put(m.getProcessId(), enderecoProcesso);

                if (Q.isEmpty()) {
                    enviarGrant(m.getProcessId(), socket);
                }

                Q.add(m.getProcessId());
            }
            else if (m.getType() == Message.RELEASE) {
                Q.poll();

                if (!Q.isEmpty()) {
                    enviarGrant(Q.peek(), socket);
                }
            }
        }
    }

    private static void enviarGrant(int processId, DatagramSocket socket) {
        try {
            InetSocketAddress destino = enderecosProcessos.get(processId);

            if (destino == null) {
                System.err.println("ERRO: Endereço do processo " + processId + " não encontrado!");
                return;
            }

            Message grant = new Message(Message.GRANT, processId);
            byte[] data = grant.transformaMessage().getBytes();

            DatagramPacket resp = new DatagramPacket(
                    data,
                    data.length,
                    destino.getAddress(),
                    destino.getPort()
            );

            socket.send(resp);

            incrementarContador(processId);
            writeLog(grant);
        } catch (Exception e) {
            System.err.println("Erro ao enviar GRANT para processo " + processId);
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
                case Message.REQUEST -> "REQUEST";
                case Message.GRANT -> "GRANT";
                case Message.RELEASE -> "RELEASE";
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
                System.out.println("\n========================================");
                System.out.println("Comandos disponíveis:");
                System.out.println("1) fila         - Ver processos na fila");
                System.out.println("2) contadores   - Ver grants por processo");
                System.out.println("3) sair         - Encerrar coordenador");
                System.out.println("========================================");
                System.out.print("> ");

                try {
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
                            scanner.close();
                            System.exit(0);
                            break;

                        default:
                            System.out.println("Comando não reconhecido.");
                    }
                } catch (Exception e) {
                    System.out.println("Entrada inválida. Digite um número.");
                    scanner.nextLine();
                }
            }
        }, "Thread-Interface").start();
    }

    private static void imprimirFila() {
        synchronized (Q) {
            if (Q.isEmpty()) {
                System.out.println("✓ Fila vazia.");
            } else {
                System.out.println("Fila atual (ordem): " + Q);
            }
        }
    }

    private static void imprimirContadores() {
        synchronized (Coordinator.class) {
            if (contadorDeProcessos.isEmpty()) {
                System.out.println("✓ Nenhum processo foi atendido ainda.");
            } else {
                System.out.println("\nQuantidade de GRANTs enviados:");
                System.out.println("─────────────────────────────");
                contadorDeProcessos.forEach((id, qtd) -> {
                    System.out.printf("  Processo %d → %d grants\n", id, qtd);
                });

                int total = contadorDeProcessos.values().stream()
                        .mapToInt(Integer::intValue).sum();
                System.out.println("─────────────────────────────");
            }
        }
    }
}