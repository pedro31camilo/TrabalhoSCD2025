import java.io.IOException;
import java.net.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Processo {
    private static final String coordinatorIp = "127.0.0.1";
    private static final int coordinatorPort = 5000;
    private static final String RESULT_FILE = "resultado.txt";
    private static final int timeout = 5000;

    private final Lock rcLock = new ReentrantLock();
    private final Condition grantReceived = rcLock.newCondition();
    private volatile boolean isRunning = true;

    private DatagramSocket socket;
    private InetSocketAddress coordDestination;

    private final int processId;
    private final int repetitions;
    private final int sleepTime;
    private volatile Message receivedGrantMessage = null;

    public Processo(int processId, int reps, int sleepT){
        this.processId = processId;
        this.repetitions = reps;
        this.sleepTime = sleepT;
    }

    private void startProcess(){
        System.out.println("Processo " + processId + " iniciado.");
        try(DatagramSocket processStartSocket = new DatagramSocket()){
            this.socket = processStartSocket;
            InetAddress coordAddress = InetAddress.getByName(coordinatorIp);
            this.coordDestination = new InetSocketAddress(coordAddress, coordinatorPort);

            System.out.println("Processo "+processId+" na porta "+socket.getLocalPort());

            new Thread(new GrantReceiverTask(), "Receiver-" + processId).start();

            for(int i = 0; i < repetitions; i++){
                sendRequest(i+1, repetitions);

                waitForGrant();

                if (grantReceived.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    Message grantMsgToUse = receivedGrantMessage;

                    receivedGrantMessage = null;

                    System.out.println("Processo " + processId + " recebeu GRANT. Entrando na RC");

                    executeCriticalRegion(grantMsgToUse);
                }

                sendRelease();
            }
            System.out.println("Repetições concluídas. Processo" + processId+" encerrando...");
        } catch (IOException e) {
            System.err.printf("Processo %d: Erro de comunicação ou inicialização: %s\n", processId, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("Processo %d: Interrompido durante a espera do GRANT.\n", processId);
        } finally {
            isRunning = false; // Sinaliza o fim para a thread de recepção
            if (socket != null && !socket.isClosed()) {
                socket.close(); // Fecha o socket, o que desbloqueia a thread receptora
            }
        }
    }

    private class GrantReceiverTask implements Runnable{
        @Override
        public void run() {
            byte[] buffer = new byte[50];
            DatagramPacket respCoordGrant = new DatagramPacket(buffer, buffer.length);

            try{
                while (isRunning){
                    socket.receive(respCoordGrant);
                    String respData = new String(respCoordGrant.getData(), 0, respCoordGrant.getLength());
                    Message coordGrant = Message.transformaString(respData);

                    if(coordGrant != null && coordGrant.getType() == Message.GRANT){
                        receivedGrantMessage = coordGrant;
                        rcLock.lock();
                        try{
                            grantReceived.signal();
                        } finally {
                            rcLock.unlock();
                        }
                    } else if(coordGrant != null){
                        System.out.println("Processo "+processId+" recebeu uma mensagem inesperada do Coordenador");
                    }
                }
            } catch (SocketException e) {
                // Esta exceção é esperada quando o socket é fechado externamente (no bloco finally de start())
                if (isRunning) {
                    System.err.printf("Processo %d: Erro no socket de recepção: %s\n", processId, e.getMessage());
                }
            } catch (IOException e) {
                System.err.printf("Processo %d: Erro de IO na recepção: %s\n", processId, e.getMessage());
            }
            System.out.printf("Processo %d: Thread de Recepção encerrada.\n", processId);
        }
    }

    private void sendRequest(int current, int total) throws IOException {
        Message processRequest = new Message(Message.REQUEST, processId);
        sendPacket(processRequest);
        System.out.println("Processo "+processId+" enviou REQUEST. ("+current+"/"+total+")");
    }

    private void sendPacket(Message m) throws IOException {
        //Função para enviar pacote para o coordenador
        String data = m.transformaMessage();
        byte[] buffer = data.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, coordDestination);
        socket.send(packet);
    }

    private void waitForGrant() throws InterruptedException {
        rcLock.lock();
        try {
            System.out.println("Processo "+processId+" aguardando GRANT.");
            if(!grantReceived.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)){
                throw new InterruptedException("TIMEOUT");
            }
        } finally {
            rcLock.unlock();
        }
    }

    private void executeCriticalRegion(Message m){
        System.out.println("Processo "+processId+" entrando na Região Crítica.");
        try{
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

            FileHelper.appendToFile(linha, RESULT_FILE);

            Thread.sleep(sleepTime);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendRelease() throws IOException{
        Message processRelease = new Message(Message.RELEASE, processId);
        sendPacket(processRelease);
        System.out.println("Processo "+processId+" enviou RELEASE. Saindo da RC.");
    }

    static void main() {
        int processId = 1;
        int reps = 5;
        int sleepProcess = 1000;

        new Processo(processId, reps, sleepProcess).startProcess();
    }
}
