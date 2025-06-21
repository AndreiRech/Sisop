package os;

import utils.GlobalVariables;
import utils.PCB;
import hardware.HW;
import enums.ProcessStates;

public class Scheduler {
    private HW hw;

    public Scheduler(HW hw) {
        this.hw = hw;
    }

    public void roundRobin() throws InterruptedException {
        while (!GlobalVariables.shutdown) {
            GlobalVariables.semaphoreScheduler.acquire();

            if (GlobalVariables.running.getId() != -1 && GlobalVariables.running.getState() != ProcessStates.finished) {
                GlobalVariables.running.setContext(hw.cpu.pc, hw.cpu.reg);

                GlobalVariables.running.setStates(ProcessStates.ready);

                GlobalVariables.ready.add(GlobalVariables.running);
                System.out.println("Processo " + GlobalVariables.running.getId() + " retornou para a fila de prontos.");
            }

            if (GlobalVariables.ready.isEmpty()) {
                System.out.println("\nSem processos prontos para execução.");
                GlobalVariables.running = GlobalVariables.nop;
                GlobalVariables.semaphoreCPU.release();
                continue;
            }
            
            GlobalVariables.running = GlobalVariables.ready.poll();
            GlobalVariables.running.setStates(ProcessStates.running);
            hw.cpu.setContext(GlobalVariables.running.getPc(), GlobalVariables.running.getRegState());

            GlobalVariables.semaphoreCPU.release();
        }
    }
}
