package SimulatedOS.Hardware;

import SimulatedOS.Utils.Opcode;
import SimulatedOS.Utils.Word;

public class Memory {
	public Word[] pos; // pos[i] é a posição i da memória. cada posição é uma palavra.

	public Memory(int size) {
		pos = new Word[size];
		for (int i = 0; i < pos.length; i++) {
			pos[i] = new Word(Opcode.___, -1, -1, -1);
		}
	}
}