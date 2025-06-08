package SimulatedOS.threads;

import SimulatedOS.OS.Scheduler;

public class SchedulerRunnable implements Runnable {
    private Scheduler scheduler;

    public SchedulerRunnable(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void run() {
        System.out.println("Thread Scheduler em execução.");
        try {
            scheduler.roundRobin();
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
    }
}
