package SimulatedOS.threads;

import SimulatedOS.OS.VM.VmIo;

public class VmIoRunnable implements Runnable {
	private VmIo vmIo;

	public VmIoRunnable(VmIo vmIo) {
		this.vmIo = vmIo;
	}

	public void run() {
		System.out.println("Thread VmIo em execução.");
		try {
			vmIo.run();
		} catch (InterruptedException e) {
			// e.printStackTrace();
		}
	}
}
