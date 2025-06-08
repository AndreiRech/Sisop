package SimulatedOS.Hardware;

import SimulatedOS.Utils.GlobalVariables;
import SimulatedOS.threads.CpuRunnable;

public class HW {
    public Memory memory;
    public Disk disk;
    public CPU cpu;

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
