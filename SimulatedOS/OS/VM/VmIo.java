package SimulatedOS.OS.VM;

import SimulatedOS.Utils.GlobalVariables;
import SimulatedOS.enums.Interrupts;

public class VmIo {
    public void run() throws InterruptedException {
        while (!GlobalVariables.shutdown) {
            GlobalVariables.semaphoreVm.acquire();
            System.out.println("------------------------------------- [ VMIO ] -------------------------------------");

            // Serve entrada de pagina -> frame

            // Interrompe cpu
            GlobalVariables.irpt.add(Interrupts.pageSaved);
        }
    }
}
