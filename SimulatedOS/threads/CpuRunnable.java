package SimulatedOS.threads;

import SimulatedOS.Hardware.CPU;

public class CpuRunnable implements Runnable {
    private CPU cpu;

    public CpuRunnable(CPU cpu) {
        this.cpu = cpu;
    }

    public void run() {
        System.out.println("Thread CPU em execução.");
        try {
            cpu.run();
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
    }
}
