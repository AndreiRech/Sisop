package utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import enums.Interrupts;
import enums.SysCalls;
import os.vm.VmRequest;

public class GlobalVariables {
    public static volatile boolean shutdown = false;
	public static final List<Thread> workerThreads = new ArrayList<>();

	public static VmRequest vmRequest = new VmRequest();
	public static boolean ioRequest = false;

	public static Semaphore semaphoreScheduler = new Semaphore(0);
	public static Semaphore semaphoreConsole = new Semaphore(0);
	public static Semaphore semaphoreCPU = new Semaphore(0);
	public static Semaphore semaphoreVm = new Semaphore(0);

	public static final Queue<Interrupts> irpt = new ConcurrentLinkedQueue<>();
	public static final Queue<SysCalls> sysc = new ConcurrentLinkedQueue<>();

	public static final Queue<PCB> blockedIO = new ConcurrentLinkedQueue<>();
	public static final Queue<PCB> blockedVM = new ConcurrentLinkedQueue<>();
	public static final Queue<PCB> ready = new ConcurrentLinkedQueue<>();
	
	public static PCB running = null;

	public static boolean autoMode = true;
}
