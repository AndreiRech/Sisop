package os;

import enums.ProcessStates;
import hardware.HW;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.GlobalVariables;
import utils.Opcode;
import utils.PCB;
import utils.PageTableEntry;
import utils.Word;

public class ProcessManager {
	private HW hw;
	private MemoryManager mm;
	private List<PCB> processes;
	private Map<Integer, int[]> processDiskMaps; // O mapa persistente de disco

	public ProcessManager(HW hw, MemoryManager mm) {
		this.hw = hw;
		this.mm = mm;
		this.processes = new ArrayList<>();
		this.processDiskMaps = new HashMap<>(); // Inicializa o mapa
	}

	public List<PCB> getProcesses() {
		return processes;
	}

	public int[] getDiskMapForProcess(int pcbId) {
        return this.processDiskMaps.get(pcbId);
    }

	public int getDiskPageFor(int pcbId, int pageIndex) {
		int[] diskMap = this.processDiskMaps.get(pcbId);
		if (diskMap != null && pageIndex < diskMap.length) {
			return diskMap[pageIndex];
		}
		return -1; // Página não encontrada
	}

	public PCB createProcess(Word[] program) {
		System.out.println("\n			CRIANDO PROCESSO COM MEMÓRIA VIRTUAL...");

		// ETAPA 1: Cálculo de tamanho (está correto)
		int maxLogicalAddr = program.length - 1;
		for (Word w : program) {
			switch (w.opc) {
				case LDD, STD, LDX, STX, JMPIM, JMPIGM, JMPILM, JMPIEM, JMPIGK, JMPILK, JMPIEK, JMPIGT:
					if (w.p > maxLogicalAddr)
						maxLogicalAddr = w.p;
					break;
				default:
					break;
			}
		}
		int wordsNumber = maxLogicalAddr + 1;
		int pageSize = mm.getPageSize();
		int numPages = (int) Math.ceil((double) wordsNumber / pageSize);

		// ETAPA 2: Alocação no disco (first-fit por páginas)
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

		// ETAPA 3: Preparação da Tabela de Páginas
		PageTableEntry[] pageTable = new PageTableEntry[numPages];
		for (int i = 0; i < numPages; i++) {
			pageTable[i] = new PageTableEntry();
			pageTable[i].setValid(false);
			pageTable[i].setFrame(diskPagesMap[i]); // Aponta para a página no DISCO
		}

		// [CORREÇÃO] ETAPA 4: ALOCAR UM FRAME NA RAM PARA A PÁGINA 0
		// Em vez de procurar manualmente, chama o método que força a alocação.
		int ramFrameForPage0 = mm.requestFrame(this, null, -1); // Chama com processo nulo para indicar alocação inicial
		if (ramFrameForPage0 == -1) {
			System.out.println("Gerente de Processos: Falha Crítica - Não foi possível obter um frame de memória.");
			return null;
		}
		pageTable[0].setValid(true);
		pageTable[0].setFrame(ramFrameForPage0);

		// ETAPA 5: Criação do PCB e cópia de dados
		PCB pcb = new PCB(0, pageTable);
		pcb.setContext(0, new int[10]);
		this.processDiskMaps.put(pcb.getId(), diskPagesMap); // Salva o mapa de disco

		System.out.println("				Processo " + pcb.getId() + " -> criado com PageTable (tamanho "
				+ pageTable.length + ")");
		System.out.println("				Processo " + pcb.getId() + " -> páginas alocadas no disco em: "
				+ Arrays.toString(diskPagesMap));

		for (int logicalAddr = 0; logicalAddr < program.length; logicalAddr++) {
			int page = logicalAddr / pageSize;
			int offset = logicalAddr % pageSize;
			int diskPage = diskPagesMap[page];
			int physDiskAddr = diskPage * pageSize + offset;
			hw.disk.pos[physDiskAddr] = program[logicalAddr];
		}
		System.out.println("				Processo " + pcb.getId() + " -> copiado com sucesso para o disco");

		for (int i = 0; i < pageSize && i < program.length; i++) {
			hw.memory.pos[ramFrameForPage0 * pageSize + i] = program[i];
		}
		System.out
				.println("				Processo " + pcb.getId() + " -> página 0 copiada com sucesso para a memória");

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

	public int allocPageFault(int id, int pageIndex) {
		PCB target = null;
		for (PCB pcb : processes) {
			if (pcb.getId() == id) {
				target = pcb;
				break;
			}
		}
		if (target == null)
			return -1;

		PageTableEntry faultyPageEntry = target.getPagesTable()[pageIndex];
		if (faultyPageEntry.isValid())
			return faultyPageEntry.getFrame();

		System.out.println("PAGE FAULT / Processo " + id + " - Página Lógica " + pageIndex);

		int diskPage = faultyPageEntry.getFrame();
		int newRamFrame = mm.requestFrame(this, target, pageIndex);

		int pageSize = mm.getPageSize();
		int baseDiskAddr = diskPage * pageSize;
		int baseRamAddr = newRamFrame * pageSize;
		for (int i = 0; i < pageSize; i++) {
			if ((baseDiskAddr + i) < hw.disk.pos.length) {
				hw.memory.pos[baseRamAddr + i] = hw.disk.pos[baseDiskAddr + i];
			}
		}

		faultyPageEntry.setValid(true);
		faultyPageEntry.setFrame(newRamFrame);
		System.out.println("	====> GP: Página " + pageIndex + " carregada: página " + diskPage
				+ " do DISCO para frame " + newRamFrame + " da RAM");
		return newRamFrame;
	}
}