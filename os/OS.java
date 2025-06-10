package os;

import handlers.InterruptHandling;
import handlers.SysCallHandling;
import hardware.HW;
import utils.GlobalVariables;
import utils.Utilities;
import utils.Word;
import os.vm.VmIo;
import threads.ConsoleRunnable;
import threads.SchedulerRunnable;
import threads.VmIoRunnable;
import utils.PCB;
import utils.PageTableEntry;

public class OS {
	private HW hw;
	private InterruptHandling ih;
	private SysCallHandling sc;
	public Utilities utils;
	private MemoryManager mm;
	private ProcessManager pm;
	private Scheduler scheduler;
	private Console console;
	private VmIo vmIo;

	public OS(HW hw, int tamMem, int tamPag) {
		this.hw = hw;
		utils = new Utilities(hw);
		mm = new MemoryManager(hw, tamMem, tamPag);
		pm = new ProcessManager(hw, mm);
		scheduler = new Scheduler(hw);
		console = new Console();
		vmIo = new VmIo();
		ih = new InterruptHandling(hw, pm);
		sc = new SysCallHandling(hw, pm);
		hw.cpu.setAddressOfHandlers(ih, sc);

		SchedulerRunnable schedulerRunning = new SchedulerRunnable(scheduler);
		Thread schedulerThread = new Thread(schedulerRunning);

		ConsoleRunnable consoleRunnable = new ConsoleRunnable(console);
		Thread consoleThread = new Thread(consoleRunnable);

		VmIoRunnable vmIoRunnable = new VmIoRunnable(vmIo);
		Thread vmIoThread = new Thread(vmIoRunnable);

		GlobalVariables.workerThreads.add(schedulerThread);
		GlobalVariables.workerThreads.add(consoleThread);
		GlobalVariables.workerThreads.add(vmIoThread);
		schedulerThread.start();
		consoleThread.start();
		vmIoThread.start();
	}

	// cria um processo na memória
	public boolean newProcess(Word[] program) {
		GlobalVariables.running = pm.createProcess(program);

		return GlobalVariables.running == null;
	}

	// lista todos processos existentes
	public void ps() {
		for (PCB pcb : pm.getProcesses()) {
			System.out.println(pcb);
		}
	}

	public void execAll() {
		if (GlobalVariables.ready.isEmpty()) {
			System.out.println("Não há processos prontos para execução.");
			return;
		}

		GlobalVariables.semaphoreScheduler.release();
	}

	// retira o processo id do sistema, tenha ele executado ou não
	public boolean rmProcess(int id) {
		if (id < 0) {
			System.out.println("ID inválido.");
			return false;
		}

		pm.dealloc(id);
		return true;
	}

	// lista o conteúdo do PCB e o conteúdo da memória do processo com id
	public void dump(int id) {
		PCB target = null;

		for (PCB pcb : pm.getProcesses()) {
			if (pcb.getId() == id) {
				target = pcb;
				break;
			}
		}

		if (target != null) {
			PageTableEntry[] pages = target.getPagesTable();
			int pageSize = mm.getPageSize();

			System.out.println("Dump | id " + id + ":");

			for (int i = 0; i < pages.length; i++) {
				int start = pages[i].getFrame() * pageSize;
				int end = start + pageSize;
				utils.dump(start, end);
			}
		}
	}

	// Lista a memória entre posições início e fim, independente do processo
	public void dumpM(int start, int end) {
		utils.dump(start, end);
	}

	// Liga modo de execução em que CPU print cada instrução executada
	public void traceOn() {
		hw.cpu.debug = true;
	}

	// Desliga o modo acima
	public void traceOff() {
		hw.cpu.debug = false;
	}

	// public void exit() {} do sistema operacional
}