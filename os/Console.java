package os;

import java.util.Scanner;

import utils.GlobalVariables;
import utils.Opcode;
import enums.Interrupts;
import hardware.HW;
import utils.PCB;
import utils.Word;

public class Console {
    private HW hw;
    private ProcessManager pm;

     public Console(HW hw, ProcessManager pm) {
        this.hw = hw;
        this.pm = pm;
    }

    public void run() throws InterruptedException {
        while (!GlobalVariables.shutdown) {
            // Scanner in = new Scanner(System.in);

            GlobalVariables.semaphoreConsole.acquire();
            System.out.println(
                    "------------------------------------- [ CONSOLE ] -------------------------------------");

            if (GlobalVariables.ioRequest) {
                System.out.println("Console: IO leitura");

                System.out.println("                                                                                            IN:   ");
                int input = 9; // in.nextInt();

                PCB p = GlobalVariables.blockedIO.peek();

                int address = p.getRegState()[9];
                int page = address / 4;
                int offset = address % 4;

                if (!p.getPagesTable()[page].isValid()) {
                    this.pm.allocPageFault(p.getId(), page);
                }
                int frame = p.getPagesTable()[page].getFrame();
                int physicalAddress = frame * 4 + offset;

                hw.memory.pos[physicalAddress] =  new Word(Opcode.DATA, -1, -1, input);

                p.setRegState(9, input);
            } else {
                System.out.println("Console: IO escrita");

                PCB p = GlobalVariables.blockedIO.peek();

                System.out.println("                                                                                            OUT:   " + p.getRegState()[9]);
            }
            
            GlobalVariables.irpt.add(Interrupts.ioFinished);
        }
    }
}
