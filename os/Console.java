package os;

import utils.GlobalVariables;
import utils.Opcode;
import enums.Interrupts;
import hardware.HW;
import utils.PCB;

public class Console {
    private HW hw;
    private ProcessManager pm;
    private MemoryManager mm;

    public Console(HW hw, ProcessManager pm, MemoryManager mm) {
        this.hw = hw;
        this.pm = pm;
        this.mm = mm;
    }

    public void run() throws InterruptedException {
        while (!GlobalVariables.shutdown) {
            GlobalVariables.semaphoreConsole.acquire();
            System.out.println(
                    "\n------------------------------------- [ INICIO CONSOLE ] -------------------------------------\n");

            PCB p = GlobalVariables.blockedIO.peek();
            if (p == null)
                continue;

            if (GlobalVariables.ioRequest) {
                // LÓGICA DE LEITURA
                System.out.println("    ====> CONSOLE: IO leitura");

                int input = 9; // Valor de exemplo, como no log
                System.out.println("    ====> CONSOLE INPUT: " + input);

                int logicalAddress = p.getRegState()[9];
                int pageSize = mm.getPageSize(); // Pega o pageSize através do PM -> MM

                // [CORREÇÃO FINAL] Escreve o valor lido DIRETAMENTE na imagem do processo no
                // DISCO
                try {
                    int[] diskMap = pm.getDiskMapForProcess(p.getId());
                    if (diskMap != null) {
                        int page = logicalAddress / pageSize;
                        int offset = logicalAddress % pageSize;
                        int diskPage = diskMap[page];
                        int physDiskAddr = diskPage * pageSize + offset;

                        // Escreve o dado lido na posição correta do disco
                        hw.disk.pos[physDiskAddr].opc = Opcode.DATA;
                        hw.disk.pos[physDiskAddr].p = input;

                        System.out.println("    ====> CONSOLE: Valor " + input + " escrito no endereço lógico "
                                + logicalAddress + " (Disco Pág:" + diskPage + ", Offset:" + offset + ")");
                    } else {
                        System.out
                                .println("    ERRO CONSOLE: Não foi possível encontrar o mapa de disco para o processo "
                                        + p.getId());
                    }
                } catch (Exception e) {
                    System.out.println(
                            "    ERRO CONSOLE: Falha ao tentar escrever no disco. Endereço lógico: " + logicalAddress);
                    e.printStackTrace();
                }

            } else {
                // LÓGICA DE ESCRITA
                System.out.println("    CONSOLE: IO escrita");
                System.out.println("    CONSOLE OUTPUT:   " + p.getRegState()[9]);
            }

            GlobalVariables.irpt.add(Interrupts.ioFinished);

            System.out.println(
                    "\n------------------------------------- [ FIM CONSOLE ] -------------------------------------\n\n");
        }
    }
}
