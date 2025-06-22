package os;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hardware.HW;
import utils.GlobalVariables;
import utils.Opcode;
import utils.Word;
import enums.ProcessStates;
import utils.PCB;
import utils.PageTableEntry;

public class ProcessManager {
	private HW hw;
	private MemoryManager mm;
	private List<PCB> processes;

	public ProcessManager(HW hw, MemoryManager mm) {
		this.hw = hw;
		this.mm = mm;
		this.processes = new ArrayList<>();
	}

	public List<PCB> getProcesses() {
		return processes;
	}

	public PCB createProcess(Word[] program) {
		System.out.println("\n			CRIANDO PROCESSO COM MEMÓRIA VIRTUAL...");

		int maxLogicalAddr = program.length - 1;
		for (Word w : program) {
			switch (w.opc) {
				case LDD, STD, LDX, STX, JMPIM, JMPIGM, JMPILM, JMPIEM, JMPIGK, JMPILK, JMPIEK, JMPIGT:
					if (w.p > maxLogicalAddr) {
						maxLogicalAddr = w.p;
					}
					break;
				default:
					break;
			}
		}

		int wordsNumber = maxLogicalAddr + 1;
		int pageSize = mm.getPageSize();
		int numPages = (int) Math.ceil((double) wordsNumber / pageSize);

		// ALOCAR PÁGINAS NO DISCO (FIRST-FIT)
		int[] diskPagesMap = new int[numPages];
		Arrays.fill(diskPagesMap, -1);
		int pagesFound = 0;
		for (int diskPageIdx = 0; diskPageIdx < (hw.disk.pos.length / pageSize)
				&& pagesFound < numPages; diskPageIdx++) {
			if (hw.disk.pos[diskPageIdx * pageSize].opc == Opcode.___) {
				diskPagesMap[pagesFound++] = diskPageIdx;
			}
		}

		if (pagesFound < numPages) {
			System.out.println("Gerente de Processos: Falha - Espaço insuficiente no disco.");
			return null;
		}

		PageTableEntry[] pageTable = new PageTableEntry[numPages];
		for (int i = 0; i < numPages; i++) {
			pageTable[i] = new PageTableEntry();
			pageTable[i].setValid(false);
			pageTable[i].setFrame(diskPagesMap[i]); // Aponta para a página no DISCO
		}

		// ALOCAR UM FRAME NA RAM PARA A PÁGINA 0
		int ramFrameForPage0 = -1;
		for (int i = 0; i < mm.framesNumber; i++) {
			if (!mm.allocatedFrames[i]) {
				ramFrameForPage0 = i;
				mm.allocatedFrames[i] = true;
				mm.frameQueue.add(i);
				break;
			}
		}

		if (ramFrameForPage0 == -1) {
			System.out.println("Gerente de Processos: Falha - Memória RAM cheia, impossível alocar página 0.");
			return null;
		}

		// Atualiza a entrada da página 0 para refletir sua presença na RAM
		pageTable[0].setValid(true);
		pageTable[0].setFrame(ramFrameForPage0); // Agora aponta para o frame da RAM

		// CRIAR PCB E COPIAR OS DADOS
		PCB pcb = new PCB(0, pageTable);
		pcb.setContext(0, new int[10]);

		System.out.println("				Processo " + pcb.getId() + " -> criado com PageTable (tamanho "
				+ pageTable.length + ")");
		System.out.println("				Processo " + pcb.getId() + " -> páginas alocadas no disco em: "
				+ Arrays.toString(diskPagesMap));

		// Copia todo o programa para as páginas alocadas no DISCO
		for (int logicalAddr = 0; logicalAddr < program.length; logicalAddr++) {
			int page = logicalAddr / pageSize;
			int offset = logicalAddr % pageSize;
			int diskPage = diskPagesMap[page];
			int physDiskAddr = diskPage * pageSize + offset;
			hw.disk.pos[physDiskAddr] = program[logicalAddr];
		}
		System.out.println("				Processo " + pcb.getId() + " -> copiado com sucesso para o disco");

		// Copia SOMENTE a página 0 para o seu frame alocado na RAM
		for (int i = 0; i < pageSize && i < program.length; i++) {
			hw.memory.pos[ramFrameForPage0 * pageSize + i] = program[i];
		}
		System.out
				.println("				Processo " + pcb.getId() + " -> página 0 copiada com sucesso para a memória");

		// ADICIONAR PROCESSO À FILA DE PRONTOS
		processes.add(pcb);
		GlobalVariables.ready.add(pcb);
		pcb.setStates(ProcessStates.ready);

		return pcb;
	}

	public void dealloc(int id) {
		PCB target = null;

		for (PCB pcb : processes) {
			if (pcb.getId() == id) {
				target = pcb;
				break;
			}
		}

		if (target == null) {
			System.out.println("Gerente de Processos: Processo não encontrado.");
			return;
		}

		processes.remove(target);
		GlobalVariables.ready.remove(target);
		target.setStates(ProcessStates.finished);
	}

	// TODO: ANALISAR ESSE MÉTODO
	public int allocPageFault(int id, int pageIndex) {
		PCB target = null;

		for (PCB pcb : processes) {
			if (pcb.getId() == id) {
				target = pcb;
				break;
			}
		}

		if (target == null) {
			System.out.println("Gerente de Processos: Processo não encontrado em Page Fault.");
			return -1;
		}

		PageTableEntry faultyPageEntry = target.getPagesTable()[pageIndex];
		if (faultyPageEntry.isValid()) {
			System.out
					.println("--> Alerta: Page fault para uma página já válida? Frame: " + faultyPageEntry.getFrame());
			return faultyPageEntry.getFrame();
		}

		System.out.println("PAGE FAULT / Processo " + id + " - Página Lógica " + pageIndex);

		// 1. Obter a localização da página no disco. Ela está no campo 'frame' da PTE
		// inválida.
		int diskPage = faultyPageEntry.getFrame();

		// 2. Solicitar um quadro livre na memória RAM.
		int newRamFrame = mm.requestFrame(processes);

		// 3. Carregar os dados da página do disco para o novo quadro na RAM.
		int pageSize = mm.getPageSize();
		int baseDiskAddr = diskPage * pageSize;
		int baseRamAddr = newRamFrame * pageSize;
		for (int i = 0; i < pageSize; i++) {
			hw.memory.pos[baseRamAddr + i] = hw.disk.pos[baseDiskAddr + i];
		}

		// 4. Atualizar a Tabela de Páginas: a página agora é válida e aponta para o
		// quadro na RAM.
		faultyPageEntry.setValid(true);
		faultyPageEntry.setFrame(newRamFrame);

		System.out.println("Página " + pageIndex + " carregada do disco (pág. " + diskPage + ") para o frame da RAM "
				+ newRamFrame);

		return newRamFrame;
	}
}