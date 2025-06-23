package os;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import hardware.HW;
import utils.PCB;
import utils.PageTableEntry;

public class MemoryManager {
    public final HW hw;
    public final int pageSize;
    public final int framesNumber;
    public final boolean[] allocatedFrames;
    public final Queue<Integer> frameQueue = new LinkedList<>();

    public MemoryManager(HW hw, int memorySize, int pageSize) {
        this.hw = hw;
        this.pageSize = pageSize;
        this.framesNumber = memorySize / pageSize;
        this.allocatedFrames = new boolean[framesNumber];
        Arrays.fill(this.allocatedFrames, false);
    }

    public int getPageSize() {
        return pageSize;
    }

    // Este método é chamado apenas uma vez na criação do processo
    public int allocFirstFrame() {
        for (int i = 0; i < framesNumber; i++) {
            if (!allocatedFrames[i]) {
                allocatedFrames[i] = true;
                frameQueue.add(i);
                return i;
            }
        }
        return -1; // Sem memória
    }
    
    public void free(PageTableEntry[] pageTable) {
        if (pageTable == null) return;
        for (PageTableEntry entry : pageTable) {
            if (entry.isValid()) {
                int frame = entry.getFrame();
                if (frame >= 0 && frame < allocatedFrames.length) {
                    allocatedFrames[frame] = false;
                    frameQueue.remove(frame); // Remove da fila FIFO também
                }
            }
        }
    }

    // O método central para obter um frame, fazendo swap-out se necessário.
    public int requestFrame(ProcessManager pm, PCB pcbInNeed, int pageInNeed) {
        // Tenta encontrar um frame livre primeiro
        for (int i = 0; i < framesNumber; i++) {
            if (!allocatedFrames[i]) {
                allocatedFrames[i] = true;
                frameQueue.add(i);
                return i;
            }
        }

        // Se não há frames livres, inicia a substituição (FIFO)
        System.out.println("\n	SUBSTITUIÇÃO DE PÁGINA: Sem frames disponíveis. Vitimando frame mais antigo...");
        int victimFrame = frameQueue.poll(); // Pega o frame mais antigo

        // Encontra o processo e a página que estão ocupando o frame vítima
        for (PCB pcbOwner : pm.getProcesses()) {
            PageTableEntry[] ownerPageTable = pcbOwner.getPagesTable();
            for (int i = 0; i < ownerPageTable.length; i++) {
                PageTableEntry entry = ownerPageTable[i];
                if (entry.isValid() && entry.getFrame() == victimFrame) {
                    System.out.println("	-> Vitimando página " + i + " do processo " + pcbOwner.getId() + " (do frame " + victimFrame + ")");

                    // Supondo que as páginas não são "sujas" (modificadas), não precisamos salvá-las
                    // de volta no disco, pois a versão no disco já é a correta.
                    // Isso é uma simplificação comum.
                    
                    // Apenas invalida a página. O campo .frame JÁ CONTÉM a localização no disco.
                    // Não podemos sobrescrevê-lo com -1.
                    entry.setValid(false);
                    // O valor do frame não é alterado, pois ele já aponta para a localização no disco.
                    // A lógica do 'createProcess' já garante isso. Quando a página é carregada na RAM,
                    // o frame é ATUALIZADO para o frame da RAM. Quando é vitimada, ela apenas se torna
                    // inválida, e o valor do frame volta a significar "página de disco".
                    
                    frameQueue.add(victimFrame); // Adiciona o frame liberado de volta ao final da fila
                    return victimFrame;
                }
            }
        }
        
        // Se, por algum motivo, um frame da fila não pertencer a ninguém (não deveria acontecer)
        frameQueue.add(victimFrame);
        return victimFrame;
    }
}