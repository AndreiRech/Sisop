package SimulatedOS.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import SimulatedOS.OS.PCB;
import SimulatedOS.OS.VM.VmRequisition;
import SimulatedOS.enums.Interrupts;
import SimulatedOS.enums.SysCalls;

public class GlobalVariables {
    public static volatile boolean shutdown = false;
	public static final List<Thread> workerThreads = new ArrayList<>();

	public static boolean ioRequest = false;
	public static VmRequisition vmRequisition = null;

	public static Semaphore semaphoreCPU = new Semaphore(0);
	public static Semaphore semaphoreScheduler = new Semaphore(0);
	public static Semaphore semaphoreConsole = new Semaphore(0);
	public static Semaphore semaphoreVm = new Semaphore(0);

	public static final Queue<Interrupts> irpt = new LinkedList<>();
	public static final Queue<SysCalls> sysc = new LinkedList<>();

	public static final Queue<PCB> ready = new LinkedList<>();
	public static final Queue<PCB> blockedIO = new LinkedList<>();
	public static final Queue<PCB> blockedVM = new LinkedList<>();
	
	public static PCB running = null;

	public static boolean autoMode = true;
}
