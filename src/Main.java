public class Main {
    public static void main(String[] args) {
        int reps = 5;
        int sleepProcess = 2500;

        Thread[] threads = new Thread[5];

        for (int id = 0; id < 5; id++) {
            final int processId = id;
            threads[id] = new Thread(() -> {
                new Processo(processId, reps, sleepProcess).startProcess();
            }, "Process-" + id);
            threads[id].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n=== Todos os processos finalizados ===");
    }
}
