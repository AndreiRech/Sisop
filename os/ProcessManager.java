package os;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hardware.HW;
import utils.GlobalVariables;
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
			System.out.println("CRIANDO PROCESSO...");

			int maxLogicalAddr = program.length - 1;
			for (Word w : program) {
				switch (w.opc) {
					case STD, LDD, LDX, STX, JMPIM, JMPIGM, JMPILM, JMPIEM:
						if (w.p > maxLogicalAddr)
							maxLogicalAddr = w.p;
						break;
					default:
						break;
				}
			}

			int wordsNumber = maxLogicalAddr + 1;
			PageTableEntry[] pageTable = mm.alloc(wordsNumber);

			if (pageTable == null || pageTable.length == 0 || !pageTable[0].isValid()) {
				System.out.println("Gerente de Processos: Falha ao alocar página 0.");
				return null;
			}

			int pc = 0;
			int pageSize = mm.getPageSize();
			PCB pcb = new PCB(pc, pageTable);
			pcb.setContext(pc, new int[10]);

			System.out
					.println("PC: " + pc + " | PageSize: " + pageSize + " | PagesTable: " + Arrays.toString(pageTable));

			int logicalAddr = 0;
			for (Word w : program) {
				int page = logicalAddr / pageSize;
				int offset = logicalAddr % pageSize;
				int physDisk = page * pageSize + offset;

				hw.disk.pos[physDisk].opc = w.opc;
				hw.disk.pos[physDisk].ra = w.ra;
				hw.disk.pos[physDisk].rb = w.rb;
				hw.disk.pos[physDisk].p = w.p;

				// TODO: DEVE estar fora do loop, pq é apenas a primeira página que é alocada na
				// RAM
				// Se a página estiver na RAM (somente a 0 no início), também copia para a RAM
				if (pageTable[page].isValid()) {
					int frame = pageTable[page].getFrame();
					int physRam = frame * pageSize + offset;

					hw.memory.pos[physRam].opc = w.opc;
					hw.memory.pos[physRam].ra = w.ra;
					hw.memory.pos[physRam].rb = w.rb;
					hw.memory.pos[physRam].p = w.p;
				}

				logicalAddr++;
			}

			processes.add(pcb);
			GlobalVariables.ready.add(pcb);
			pcb.setStates(ProcessStates.ready);
			if (GlobalVariables.autoMode)
				GlobalVariables.semaphoreScheduler.release();

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

			mm.free(target.getPagesTable());

			processes.remove(target);
			GlobalVariables.ready.remove(target);
			target.setStates(ProcessStates.finished);
		}

		public int allocPageFault(int id, int page) {
			PCB target = null;

			for (PCB pcb : processes) {
				if (pcb.getId() == id) {
					target = pcb;
					System.out.println("Achamos!");
					break;
				}
			}

			if (target == null) {
				System.out.println("Gerente de Processos: Processo não encontrado.");
				return -1;
			}

			int pageSize = mm.getPageSize();
			int pageIndex = page;

			System.out.println("PAGE FAULT no processo " + id + ", página " + pageIndex);

			int frame = mm.requestFrame(target, pageIndex, processes);
			target.getPagesTable()[pageIndex].setValid(true);
			target.getPagesTable()[pageIndex].setFrame(frame);

			// Copiar do disco para a RAM (swap in)
			int baseDisk = pageIndex * pageSize;
			int baseRam = frame * pageSize;

			for (int i = 0; i < pageSize; i++) {
				hw.memory.pos[baseRam + i] = hw.disk.pos[baseDisk + i];
			}

			// Bloqueia o processo
			target.setStates(ProcessStates.ready);

			System.out.println("Página " + pageIndex + " carregada para o frame " + frame);

			return frame;
		}
	}
    
