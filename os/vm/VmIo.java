package os.vm;

import utils.GlobalVariables;
import enums.Interrupts;
import os.ProcessManager;

public class VmIo {
    private ProcessManager pm;

	public VmIo(ProcessManager pm) {
		this.pm = pm;
	}

    public void run() throws InterruptedException {
        while (!GlobalVariables.shutdown) {
            GlobalVariables.semaphoreVm.acquire();
            System.out.println("------------------------------------- [ VMIO ] -------------------------------------");

            // Serve entrada de pagina -> frame
            this.pm.allocPageFault(GlobalVariables.vmRequest.getProcessId(), GlobalVariables.vmRequest.getRequestedPage());

            // Interrompe cpu
            GlobalVariables.irpt.add(Interrupts.pageSaved);
        }
    }
}
