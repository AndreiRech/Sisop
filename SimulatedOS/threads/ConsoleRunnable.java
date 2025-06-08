package SimulatedOS.threads;

import SimulatedOS.OS.Console;

public class ConsoleRunnable implements Runnable {
    private Console console;

    public ConsoleRunnable(Console console) {
        this.console = console;
    }

    public void run() {
        System.out.println("Thread Console em execução.");
        try {
            console.run();
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
    }
}
