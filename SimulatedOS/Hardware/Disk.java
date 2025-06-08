package SimulatedOS.Hardware;

import SimulatedOS.Utils.Opcode;
import SimulatedOS.Utils.Word;

public class Disk {
	public Word[] pos;

	public Disk(int size) {
		pos = new Word[size];
		for (int i = 0; i < pos.length; i++) {
			pos[i] = new Word(Opcode.___, -1, -1, -1);
		}
	}
}