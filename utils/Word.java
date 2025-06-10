package utils;

public class Word { // cada posicao da memoria tem uma instrucao (ou um dado)
    public Opcode opc; //
    public int ra; // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
    public int rb; // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
    public int p; // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

    public Word(Opcode _opc, int _ra, int _rb, int _p) { // vide definição da VM - colunas vermelhas da tabela
        opc = _opc;
        ra = _ra;
        rb = _rb;
        p = _p;
    }
}
