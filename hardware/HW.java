package hardware;

import utils.GlobalVariables;
import threads.CpuRunnable;

public class HW {
    public final Memory memory;
    public final Disk disk;
    public final CPU cpu;

    public HW(int memorySize, int diskSize) {
        memory = new Memory(memorySize);
        disk = new Disk(diskSize);
        cpu = new CPU(memory, disk, true); // true liga debug

        CpuRunnable cpuRunnable = new CpuRunnable(cpu);
        Thread cpuThread = new Thread(cpuRunnable);
        GlobalVariables.workerThreads.add(cpuThread);
        cpuThread.start();
    }
}
