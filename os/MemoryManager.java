package os;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import hardware.HW;
import utils.PCB;
import utils.PageTableEntry;

public class MemoryManager {
		private HW hw;
		private int pageSize;
		int framesNumber;
		boolean[] allocatedFrames;
		Queue<Integer> frameQueue = new LinkedList<>();

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

		public PageTableEntry[] alloc(int wordsNumber) {
			int pagesNumber = (int) Math.ceil((double) wordsNumber / this.pageSize);
			PageTableEntry[] pageTable = new PageTableEntry[pagesNumber];

			for (int i = 0; i < pagesNumber; i++) {
				pageTable[i] = new PageTableEntry();
			}

			// Aloca só a página 0
			for (int i = 0; i < framesNumber; i++) {
				if (!allocatedFrames[i]) {
					allocatedFrames[i] = true;
					frameQueue.add(i);

					pageTable[0].setValid(true);
					pageTable[0].setFrame(i);
					break;
				}
			}

			return pageTable;
		}

		public void free(PageTableEntry[] pageTable) {
			for (PageTableEntry entry : pageTable) {
				if (entry.isValid()) {
					int frame = entry.getFrame();
					allocatedFrames[frame] = false;
				}
			}
		}

		public int requestFrame(List<PCB> processes) {
			for (int i = 0; i < framesNumber; i++) {
				if (!allocatedFrames[i]) {
					allocatedFrames[i] = true;
					frameQueue.add(i);
					return i;
				}
			}

			System.out.println("Sem frames disponíveis. Iniciando substituição de página...");
			int victimFrame = frameQueue.poll();
			allocatedFrames[victimFrame] = true;

			for (PCB pcb : processes) {
				for (PageTableEntry entry : pcb.getPagesTable()) {
					if (entry.isValid() && entry.getFrame() == victimFrame) {

						int page = Arrays.asList(pcb.getPagesTable()).indexOf(entry);
						int baseRam = victimFrame * pageSize;
						int baseDisk = page * pageSize;

						for (int i = 0; i < pageSize; i++) {
							hw.disk.pos[baseDisk + i] = hw.memory.pos[baseRam + i];
						}

						entry.setValid(false);
						entry.setFrame(-1);
						break;
					}
				}
			}

			frameQueue.add(victimFrame);
			return victimFrame;
		}
	}
    
