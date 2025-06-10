package utils;
// ------------------ U T I L I T A R I O S D O S I S T E M A

// -----------------------------------------
// ------------------ load é invocado a partir de requisição do usuário

import hardware.HW;

public class Utilities {
    private HW hw;

    public Utilities(HW hw) {
        this.hw = hw;
    }

    // dump da memória
    public void dump(Word w) { // funcoes de DUMP nao existem em hardware - colocadas aqui para facilidade
        System.out.print("[ ");
        System.out.print(w.opc);
        System.out.print(", ");
        System.out.print(w.ra);
        System.out.print(", ");
        System.out.print(w.rb);
        System.out.print(", ");
        System.out.print(w.p);
        System.out.println("  ] ");
    }

    public void dump(int ini, int fim) {
        Word[] m = hw.memory.pos; // m[] é o array de posições memória do hw
        for (int i = ini; i < fim; i++) {
            System.out.print(i);
            System.out.print(":  ");
            dump(m[i]);
        }
    }
}
