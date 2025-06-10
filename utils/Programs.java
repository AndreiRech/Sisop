package utils;

public class Programs {

        public Word[] retrieveProgram(String pname) {
                for (Program p : progs) {
                        if (p != null && p.name.equals(pname))
                                return p.image;
                }
                return null;
        }

        public Program[] progs = {
                        new Program("fatorial",
                                        new Word[] {
                                                        // este fatorial so aceita valores positivos. nao pode ser zero
                                                        // linha coment
                                                        new Word(Opcode.LDI, 0, -1, 7), // 0 r0 é valor a calcular
                                                                                        // fatorial
                                                        new Word(Opcode.LDI, 1, -1, 1), // 1 r1 é 1 para multiplicar
                                                                                        // (por r0)
                                                        new Word(Opcode.LDI, 6, -1, 1), // 2 r6 é 1 o decremento
                                                        new Word(Opcode.LDI, 7, -1, 8), // 3 r7 tem posicao 8 para fim
                                                                                        // do programa
                                                        new Word(Opcode.JMPIE, 7, 0, 0), // 4 se r0=0 pula para r7(=8)
                                                        new Word(Opcode.MULT, 1, 0, -1), // 5 r1 = r1 * r0 (r1 acumula o
                                                                                         // produto por cada termo)
                                                        new Word(Opcode.SUB, 0, 6, -1), // 6 r0 = r0 - r6 (r6=1)
                                                                                        // decrementa r0 para proximo
                                                                                        // termo
                                                        new Word(Opcode.JMP, -1, -1, 4), // 7 vai p posicao 4
                                                        new Word(Opcode.STD, 1, -1, 10), // 8 coloca valor de r1 na
                                                                                         // posição 10
                                                        new Word(Opcode.STOP, -1, -1, -1), // 9 stop
                                                        new Word(Opcode.DATA, -1, -1, -1) // 10 ao final o valor está na
                                                                                          // posição 10 da memória
                                        }),

                        new Program("fatorialV2",
                                        new Word[] {
                                                        new Word(Opcode.LDI, 0, -1, 5), // numero para colocar na
                                                                                        // memoria, ou pode ser lido
                                                        new Word(Opcode.STD, 0, -1, 19),
                                                        new Word(Opcode.LDD, 0, -1, 19),
                                                        new Word(Opcode.LDI, 1, -1, -1),
                                                        new Word(Opcode.LDI, 2, -1, 13), // SALVAR POS STOP
                                                        new Word(Opcode.JMPIL, 2, 0, -1), // caso negativo pula pro STD
                                                        new Word(Opcode.LDI, 1, -1, 1),
                                                        new Word(Opcode.LDI, 6, -1, 1),
                                                        new Word(Opcode.LDI, 7, -1, 13),
                                                        new Word(Opcode.JMPIE, 7, 0, 0), // POS 9 pula para STD (Stop-1)
                                                        new Word(Opcode.MULT, 1, 0, -1),
                                                        new Word(Opcode.SUB, 0, 6, -1),
                                                        new Word(Opcode.JMP, -1, -1, 9), // pula para o JMPIE
                                                        new Word(Opcode.STD, 1, -1, 18),
                                                        new Word(Opcode.LDI, 8, -1, 2), // escrita
                                                        new Word(Opcode.LDI, 9, -1, 18), // endereco com valor a
                                                                                         // escrever
                                                        new Word(Opcode.SYSCALL, -1, -1, -1),
                                                        new Word(Opcode.STOP, -1, -1, -1), // POS 17
                                                        new Word(Opcode.DATA, -1, -1, -1), // POS 18
                                                        new Word(Opcode.DATA, -1, -1, -1) } // POS 19
                        ),

                        new Program("progMinimo",
                                        new Word[] {
                                                        new Word(Opcode.LDI, 0, -1, 999),
                                                        new Word(Opcode.STD, 0, -1, 8),
                                                        new Word(Opcode.STD, 0, -1, 9),
                                                        new Word(Opcode.STD, 0, -1, 10),
                                                        new Word(Opcode.STD, 0, -1, 11),
                                                        new Word(Opcode.STD, 0, -1, 12),
                                                        new Word(Opcode.STOP, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1), // 7
                                                        new Word(Opcode.DATA, -1, -1, -1), // 8
                                                        new Word(Opcode.DATA, -1, -1, -1), // 9
                                                        new Word(Opcode.DATA, -1, -1, -1), // 10
                                                        new Word(Opcode.DATA, -1, -1, -1), // 11
                                                        new Word(Opcode.DATA, -1, -1, -1), // 12
                                                        new Word(Opcode.DATA, -1, -1, -1) // 13
                                        }),

                        new Program("fibonacci10",
                                        new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
                                                        new Word(Opcode.LDI, 1, -1, 0),
                                                        new Word(Opcode.STD, 1, -1, 20),
                                                        new Word(Opcode.LDI, 2, -1, 1),
                                                        new Word(Opcode.STD, 2, -1, 21),
                                                        new Word(Opcode.LDI, 0, -1, 22),
                                                        new Word(Opcode.LDI, 6, -1, 6),
                                                        new Word(Opcode.LDI, 7, -1, 31),
                                                        new Word(Opcode.LDI, 3, -1, 0),
                                                        new Word(Opcode.ADD, 3, 1, -1),
                                                        new Word(Opcode.LDI, 1, -1, 0),
                                                        new Word(Opcode.ADD, 1, 2, -1),
                                                        new Word(Opcode.ADD, 2, 3, -1),
                                                        new Word(Opcode.STX, 0, 2, -1),
                                                        new Word(Opcode.ADDI, 0, -1, 1),
                                                        new Word(Opcode.SUB, 7, 0, -1),
                                                        new Word(Opcode.JMPIG, 6, 7, -1),
                                                        new Word(Opcode.STOP, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1), // POS 20
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1) // ate aqui - serie de
                                                                                          // fibonacci ficara armazenada
                                        }),

                        new Program("fibonacci10v2",
                                        new Word[] { // mesmo que prog exemplo, so que usa r0 no lugar de r8
                                                        new Word(Opcode.LDI, 1, -1, 0),
                                                        new Word(Opcode.STD, 1, -1, 20),
                                                        new Word(Opcode.LDI, 2, -1, 1),
                                                        new Word(Opcode.STD, 2, -1, 21),
                                                        new Word(Opcode.LDI, 0, -1, 22),
                                                        new Word(Opcode.LDI, 6, -1, 6),
                                                        new Word(Opcode.LDI, 7, -1, 31),
                                                        new Word(Opcode.MOVE, 3, 1, -1),
                                                        new Word(Opcode.MOVE, 1, 2, -1),
                                                        new Word(Opcode.ADD, 2, 3, -1),
                                                        new Word(Opcode.STX, 0, 2, -1),
                                                        new Word(Opcode.ADDI, 0, -1, 1),
                                                        new Word(Opcode.SUB, 7, 0, -1),
                                                        new Word(Opcode.JMPIG, 6, 7, -1),
                                                        new Word(Opcode.STOP, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1), // POS 20
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1) // ate aqui - serie de
                                                                                          // fibonacci ficara armazenada
                                        }),
                        new Program("fibonacciREAD",
                                        new Word[] {
                                                        // mesmo que prog exemplo, so que usa r0 no lugar de r8
                                                        new Word(Opcode.LDI, 8, -1, 1), // leitura
                                                        new Word(Opcode.LDI, 9, -1, 55), // endereco a guardar o tamanho
                                                                                         // da serie de fib a gerar
                                                                                         // - pode ser de 1 a 20
                                                        new Word(Opcode.SYSCALL, -1, -1, -1),
                                                        new Word(Opcode.LDD, 7, -1, 55),
                                                        new Word(Opcode.LDI, 3, -1, 0),
                                                        new Word(Opcode.ADD, 3, 7, -1),
                                                        new Word(Opcode.LDI, 4, -1, 36), // posicao para qual ira pular
                                                                                         // (stop) *
                                                        new Word(Opcode.LDI, 1, -1, -1), // caso negativo
                                                        new Word(Opcode.STD, 1, -1, 41),
                                                        new Word(Opcode.JMPIL, 4, 7, -1), // pula pra stop caso negativo
                                                                                          // *
                                                        new Word(Opcode.JMPIE, 4, 7, -1), // pula pra stop caso 0
                                                        new Word(Opcode.ADDI, 7, -1, 41), // fibonacci + posição do stop
                                                        new Word(Opcode.LDI, 1, -1, 0),
                                                        new Word(Opcode.STD, 1, -1, 41), // 25 posicao de memoria onde
                                                                                         // inicia a serie de
                                                                                         // fibonacci gerada
                                                        new Word(Opcode.SUBI, 3, -1, 1), // se 1 pula pro stop
                                                        new Word(Opcode.JMPIE, 4, 3, -1),
                                                        new Word(Opcode.ADDI, 3, -1, 1),
                                                        new Word(Opcode.LDI, 2, -1, 1),
                                                        new Word(Opcode.STD, 2, -1, 42),
                                                        new Word(Opcode.SUBI, 3, -1, 2), // se 2 pula pro stop
                                                        new Word(Opcode.JMPIE, 4, 3, -1),
                                                        new Word(Opcode.LDI, 0, -1, 43),
                                                        new Word(Opcode.LDI, 6, -1, 25), // salva posição de retorno do
                                                                                         // loop
                                                        new Word(Opcode.LDI, 5, -1, 0), // salva tamanho
                                                        new Word(Opcode.ADD, 5, 7, -1),
                                                        new Word(Opcode.LDI, 7, -1, 0), // zera (inicio do loop)
                                                        new Word(Opcode.ADD, 7, 5, -1), // recarrega tamanho
                                                        new Word(Opcode.LDI, 3, -1, 0),
                                                        new Word(Opcode.ADD, 3, 1, -1),
                                                        new Word(Opcode.LDI, 1, -1, 0),
                                                        new Word(Opcode.ADD, 1, 2, -1),
                                                        new Word(Opcode.ADD, 2, 3, -1),
                                                        new Word(Opcode.STX, 0, 2, -1),
                                                        new Word(Opcode.ADDI, 0, -1, 1),
                                                        new Word(Opcode.SUB, 7, 0, -1),
                                                        new Word(Opcode.JMPIG, 6, 7, -1), // volta para o inicio do loop
                                                        new Word(Opcode.STOP, -1, -1, -1), // POS 36
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1), // POS 41
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1)
                                        }),
                        new Program("PB",
                                        new Word[] {
                                                        // dado um inteiro em alguma posição de memória,
                                                        // se for negativo armazena -1 na saída; se for positivo
                                                        // responde o fatorial do
                                                        // número na saída
                                                        new Word(Opcode.LDI, 0, -1, 7), // numero para colocar na
                                                                                        // memoria
                                                        new Word(Opcode.STD, 0, -1, 50),
                                                        new Word(Opcode.LDD, 0, -1, 50),
                                                        new Word(Opcode.LDI, 1, -1, -1),
                                                        new Word(Opcode.LDI, 2, -1, 13), // SALVAR POS STOP
                                                        new Word(Opcode.JMPIL, 2, 0, -1), // caso negativo pula pro STD
                                                        new Word(Opcode.LDI, 1, -1, 1),
                                                        new Word(Opcode.LDI, 6, -1, 1),
                                                        new Word(Opcode.LDI, 7, -1, 13),
                                                        new Word(Opcode.JMPIE, 7, 0, 0), // POS 9 pula pra STD (Stop-1)
                                                        new Word(Opcode.MULT, 1, 0, -1),
                                                        new Word(Opcode.SUB, 0, 6, -1),
                                                        new Word(Opcode.JMP, -1, -1, 9), // pula para o JMPIE
                                                        new Word(Opcode.STD, 1, -1, 15),
                                                        new Word(Opcode.STOP, -1, -1, -1), // POS 14
                                                        new Word(Opcode.DATA, -1, -1, -1) // POS 15
                                        }),
                        new Program("PC",
                                        new Word[] {
                                                        // Para um N definido (10 por exemplo)
                                                        // o programa ordena um vetor de N números em alguma posição de
                                                        // memória;
                                                        // ordena usando bubble sort
                                                        // loop ate que não swap nada
                                                        // passando pelos N valores
                                                        // faz swap de vizinhos se da esquerda maior que da direita
                                                        new Word(Opcode.LDI, 7, -1, 5), // TAMANHO DO BUBBLE SORT (N)
                                                        new Word(Opcode.LDI, 6, -1, 5), // aux N
                                                        new Word(Opcode.LDI, 5, -1, 46), // LOCAL DA MEMORIA
                                                        new Word(Opcode.LDI, 4, -1, 47), // aux local memoria
                                                        new Word(Opcode.LDI, 0, -1, 4), // colocando valores na memoria
                                                        new Word(Opcode.STD, 0, -1, 46),
                                                        new Word(Opcode.LDI, 0, -1, 3),
                                                        new Word(Opcode.STD, 0, -1, 47),
                                                        new Word(Opcode.LDI, 0, -1, 5),
                                                        new Word(Opcode.STD, 0, -1, 48),
                                                        new Word(Opcode.LDI, 0, -1, 1),
                                                        new Word(Opcode.STD, 0, -1, 49),
                                                        new Word(Opcode.LDI, 0, -1, 2),
                                                        new Word(Opcode.STD, 0, -1, 50), // colocando valores na memoria
                                                                                         // até aqui - POS 13
                                                        new Word(Opcode.LDI, 3, -1, 25), // Posicao para pulo CHAVE 1
                                                        new Word(Opcode.STD, 3, -1, 99),
                                                        new Word(Opcode.LDI, 3, -1, 22), // Posicao para pulo CHAVE 2
                                                        new Word(Opcode.STD, 3, -1, 98),
                                                        new Word(Opcode.LDI, 3, -1, 38), // Posicao para pulo CHAVE 3
                                                        new Word(Opcode.STD, 3, -1, 97),
                                                        new Word(Opcode.LDI, 3, -1, 25), // Posicao para pulo CHAVE 4
                                                                                         // (não usada)
                                                        new Word(Opcode.STD, 3, -1, 96),
                                                        new Word(Opcode.LDI, 6, -1, 0), // r6 = r7 - 1 POS 22
                                                        new Word(Opcode.ADD, 6, 7, -1),
                                                        new Word(Opcode.SUBI, 6, -1, 1), // ate aqui
                                                        new Word(Opcode.JMPIEM, -1, 6, 97), // CHAVE 3 para pular quando
                                                                                            // r7 for 1 e r6 0 para
                                                                                            // interomper o loop de vez
                                                                                            // do programa
                                                        new Word(Opcode.LDX, 0, 5, -1), // r0 e ra pegando valores das
                                                                                        // posições da memoria POS
                                                                                        // 26
                                                        new Word(Opcode.LDX, 1, 4, -1),
                                                        new Word(Opcode.LDI, 2, -1, 0),
                                                        new Word(Opcode.ADD, 2, 0, -1),
                                                        new Word(Opcode.SUB, 2, 1, -1),
                                                        new Word(Opcode.ADDI, 4, -1, 1),
                                                        new Word(Opcode.SUBI, 6, -1, 1),
                                                        new Word(Opcode.JMPILM, -1, 2, 99), // LOOP chave 1 caso neg
                                                                                            // procura prox
                                                        new Word(Opcode.STX, 5, 1, -1),
                                                        new Word(Opcode.SUBI, 4, -1, 1),
                                                        new Word(Opcode.STX, 4, 0, -1),
                                                        new Word(Opcode.ADDI, 4, -1, 1),
                                                        new Word(Opcode.JMPIGM, -1, 6, 99), // LOOP chave 1 POS 38
                                                        new Word(Opcode.ADDI, 5, -1, 1),
                                                        new Word(Opcode.SUBI, 7, -1, 1),
                                                        new Word(Opcode.LDI, 4, -1, 0), // r4 = r5 + 1 POS 41
                                                        new Word(Opcode.ADD, 4, 5, -1),
                                                        new Word(Opcode.ADDI, 4, -1, 1), // ate aqui
                                                        new Word(Opcode.JMPIGM, -1, 7, 98), // LOOP chave 2
                                                        new Word(Opcode.STOP, -1, -1, -1), // POS 45
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1),
                                                        new Word(Opcode.DATA, -1, -1, -1)
                                        })
        };
}
