package SimulatedOS.OS;

import java.util.Scanner;

import SimulatedOS.Utils.GlobalVariables;
import SimulatedOS.enums.Interrupts;

public class Console {
    public void run() throws InterruptedException {
        while (!GlobalVariables.shutdown) {
            Scanner in = new Scanner(System.in);

            GlobalVariables.semaphoreConsole.acquire();
            System.out.println(
                    "------------------------------------- [ CONSOLE ] -------------------------------------");

            if (GlobalVariables.ioRequest) {
                System.out.println("Console: IO leitura");

                System.out.println("IN:   ");
                int input = in.nextInt();

                PCB p = GlobalVariables.blockedIO.peek();
                p.setRegState(9, input);

                in.close();
            } else {
                System.out.println("Console: IO escrita");

                PCB p = GlobalVariables.blockedIO.peek();

                System.out.println("OUT:   " + p.getRegState()[9]);
            }

            GlobalVariables.irpt.add(Interrupts.ioFinished);
        }
    }
}
