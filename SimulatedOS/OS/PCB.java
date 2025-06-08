package SimulatedOS.OS;

import java.util.Arrays;

import SimulatedOS.enums.ProcessStates;

public class PCB {
    private static int idCounter;
    private int id;
    private int pc;
    private int[] regState;
    private PageTableEntry[] pagesTable;
    private ProcessStates state;

    public PCB(int pc, PageTableEntry[] pagesTable) {
        idCounter++;
        this.id = idCounter;
        this.pc = pc;
        this.pagesTable = pagesTable;
        this.regState = new int[10];
    }

    public int getId() {
        return this.id;
    }

    public int getPc() {
        return this.pc;
    }

    public int[] getRegState() {
        return this.regState;
    }

    public PageTableEntry[] getPagesTable() {
        return pagesTable;
    }

    public void setStates(ProcessStates ps) {
        this.state = ps;
    }

    public ProcessStates getState() {
        return this.state;
    }

    public void setContext(int pc, int[] reg) {
        this.pc = pc;
        this.regState = reg;
    }

    public void setRegState(int reg, int value) {
        this.regState[reg] = value;
    }

    @Override
    public String toString() {
        return "PCB [id=" + id + ", pc=" + pc + ", regState=" + Arrays.toString(regState) + ", pagesTable="
                + Arrays.toString(pagesTable) + ", state=" + state + "]";
    }
}
