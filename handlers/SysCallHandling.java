package handlers;

import hardware.HW;
import utils.GlobalVariables;

import enums.ProcessStates;
import enums.SysCalls;
import os.ProcessManager;

public class SysCallHandling {
    private HW hw; // referencia ao hw se tiver que setar algo
    private ProcessManager pm;

    public SysCallHandling(HW hw, ProcessManager pm) {
        this.hw = hw;
        this.pm = pm;
    }

    public void handle(SysCalls sysc) { // chamada de sistema - suporta somente IO, com parametros - reg[8] = in ou
                                        // out e reg[9] endereco do inteiro
        System.out.println("SYSCALL pars:  " + hw.cpu.reg[8] + " / " + hw.cpu.reg[9]);

        switch (sysc) {
            case ioRequest:
                GlobalVariables.running.setContext(hw.cpu.pc, hw.cpu.reg);
                GlobalVariables.running.setStates(ProcessStates.blocked);
                GlobalVariables.ready.remove(GlobalVariables.running); 
                GlobalVariables.blockedIO.add(GlobalVariables.running);

                System.out.println("IoRequest");
                // GlobalVariables.running.setStates(ProcessStates.blocked);
                // GlobalVariables.blockedIO.add(GlobalVariables.running);
                
                switch (hw.cpu.reg[8]) {
                    case 1:
                        GlobalVariables.ioRequest = true;
                        GlobalVariables.semaphoreConsole.release();
                        break;
                    case 2:
                        GlobalVariables.ioRequest = false;
                        GlobalVariables.semaphoreConsole.release();
                        break;
                    default:
                        System.out.println("  PARAMETRO INVALIDO");
                        break;
                }

                GlobalVariables.semaphoreScheduler.release();
                break;
            case processEnd:
                System.out.println("ProcessEnd");

                pm.dealloc(GlobalVariables.running.getId());
                GlobalVariables.running.setStates(ProcessStates.finished);
                GlobalVariables.ready.remove(GlobalVariables.running);

                GlobalVariables.semaphoreScheduler.release();
                break;
            default:
                System.out.println("Syscall nao tratada");
                break;
        }
    }
}
