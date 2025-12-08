import java.io.IOException;
import java.net.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

public class Processo {
    private static final String coordinatorIp = "127.0.0.1";
    private static final int coordinatorPort = 5000;
    private static final String RESULT_FILE = "resultado.txt";
    private static final int timeout = 500000;

    private final Lock rcLock = new ReentrantLock();
    private final Condition grantArrived = rcLock.newCondition();
    private volatile boolean isRunning = true;
    private DatagramSocket socket;
    private InetSocketAddress coordDestination;
    private final int processId;
    private final int repetitions;
    private final int sleepTime;
    private Message receivedGrant = null;
    private final Random random = new Random();

    public Processo(int processId, int reps, int sleepT) {
        this.processId = processId;
        this.repetitions = reps;
        this.sleepTime = sleepT;
        try {
            this.socket = new DatagramSocket(5001 + processId);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void startProcess() {
        System.out.println("Processo " + processId + " iniciado.");
        try {
            InetAddress coordAddress = InetAddress.getByName(coordinatorIp);
            coordDestination = new InetSocketAddress(coordAddress, coordinatorPort);
            System.out.println("Processo " + processId + " usando porta " + socket.getLocalPort());

            new Thread(new GrantReceiverTask(), "Receiver-" + processId).start();

            for (int i = 0; i < repetitions; i++) {
                sendRequest(i + 1, repetitions);
                Message grantMsg = waitForGrant();
                System.out.println("Processo " + processId + " recebeu GRANT. Entrando na RC.");
                executeCriticalRegion(grantMsg);
                sendRelease();
            }

            System.out.println("Processo " + processId + " finalizado.");
        } catch (IOException e) {
            System.err.printf("Processo %d: Erro de comunicação: %s\n", processId, e.getMessage());
        } finally {
            isRunning = false;
            if (socket != null && !socket.isClosed()) socket.close();
        }
    }

    private class GrantReceiverTask implements Runnable {
        @Override
        public void run() {
            byte[] buffer = new byte[50];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                while (isRunning) {
                    socket.receive(packet);
                    String msgStr = new String(packet.getData(), 0, packet.getLength());
                    Message msg = Message.transformaString(msgStr);

                    if (msg != null && msg.getType() == Message.GRANT) {
                        rcLock.lock();
                        try {
                            receivedGrant = msg;
                            grantArrived.signal();
                        } finally {
                            rcLock.unlock();
                        }
                    } else if (msg != null) {
                        System.out.println("Processo " + processId + " recebeu mensagem inesperada.");
                    }
                }
            } catch (SocketException e) {

            } catch (IOException e) {
                System.err.println("Erro de IO na thread de recepção: " + e.getMessage());
            }
            System.out.println("Processo " + processId + ": Thread de recepção encerrada.");
        }
    }

    private Message waitForGrant() {
        rcLock.lock();
        try {
            System.out.println("Processo " + processId + " aguardando GRANT...");
            receivedGrant = null;
            if (!grantArrived.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Processo " + processId + ": TIMEOUT esperando GRANT");
            }
            return receivedGrant;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processo interrompido", e);
        } finally {
            rcLock.unlock();
        }
    }

    private void sendPacket(Message m) throws IOException {
        byte[] buffer = m.transformaMessage().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, coordDestination);
        socket.send(packet);
    }

    private void sendRequest(int current, int total) throws IOException {
        sendPacket(new Message(Message.REQUEST, processId));
        System.out.println("Processo " + processId + " enviou REQUEST (" + current + "/" + total + ")");
    }

    private void sendRelease() throws IOException {
        sendPacket(new Message(Message.RELEASE, processId));
        System.out.println("Processo " + processId + " enviou RELEASE");
    }

    private void executeCriticalRegion(Message m) {
        System.out.println("Processo " + processId + " executando RC.");
        try {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS"));
            String linha = String.format("%s | Processo %d", timestamp, processId);
            FileHelper.appendToFile(linha, RESULT_FILE);

            long randomValue = random.nextLong(4) - 2;
            Thread.sleep(sleepTime + randomValue);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}