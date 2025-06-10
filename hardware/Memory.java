package hardware;

import utils.Opcode;
import utils.Word;

public class Memory {
	public final Word[] pos; // pos[i] é a posição i da memória. cada posição é uma palavra.

	public Memory(int size) {
		pos = new Word[size];
		for (int i = 0; i < pos.length; i++) {
			pos[i] = new Word(Opcode.___, -1, -1, -1);
		}
	}
}