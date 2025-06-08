package SimulatedOS.Hardware;

import SimulatedOS.Handlers.InterruptHandling;
import SimulatedOS.Handlers.SysCallHandling;
import SimulatedOS.Utils.GlobalVariables;
import SimulatedOS.Utils.Opcode;
import SimulatedOS.Utils.Utilities;
import SimulatedOS.Utils.Word;
import SimulatedOS.enums.Interrupts;
import SimulatedOS.enums.SysCalls;

public class CPU {
    private static final int Q = 4;
    private int maxInt; // valores maximo e minimo para inteiros nesta cpu
    private int minInt;
    // CONTEXTO da CPU ...
    public int pc; // ... composto de program counter,
    private Word ir; // instruction register,
    public int[] reg; // registradores da CPU
                      // FIM CONTEXTO DA CPU: tudo que precisa sobre o estado de um processo para
                      // executa-lo
                      // nas proximas versoes isto pode modificar

    private Word[] m; // m é o array de memória "física", CPU tem uma ref a m para acessar
    private Word[] d; // d é o array de disco "físico", CPU tem uma ref a d para acessar

    private InterruptHandling ih; // significa desvio para rotinas de tratamento de Int - se int ligada, desvia
    private SysCallHandling sysCall; // significa desvio para tratamento de chamadas de sistema

    // auxilio aa depuração
    public boolean debug; // se true entao mostra cada instrucao em execucao
    private Utilities u; // para debug (dump)

    public CPU(Memory mem, Disk disk, boolean debug) { // ref a MEMORIA passada na criacao da CPU
        maxInt = 32767; // capacidade de representacao modelada
        minInt = -32767; // se exceder deve gerar interrupcao de overflow
        m = mem.pos; // usa o atributo 'm' para acessar a memoria, só para ficar mais pratico
        d = disk.pos; // usa o atributo 'd' para acessar o disco, só para ficar mais pratico
        reg = new int[10]; // aloca o espaço dos registradores - regs 8 e 9 usados somente para IO

        this.debug = debug; // se true, print da instrucao em execucao

    }

    public void setAddressOfHandlers(InterruptHandling _ih, SysCallHandling _sysCall) {
        ih = _ih; // aponta para rotinas de tratamento de int
        sysCall = _sysCall; // aponta para rotinas de tratamento de chamadas de sistema
    }

    public void setUtilities(Utilities _u) {
        u = _u; // aponta para rotinas utilitárias - fazer dump da memória na tela
    }

    // verificação de enderecamento
    private boolean legal(int e) { // todo acesso a memoria tem que ser verificado se é válido -
                                   // aqui no caso se o endereco é um endereco valido em toda memoria
        if (e >= 0 && e < m.length) {
            return true;
        } else {
            GlobalVariables.irpt.add(Interrupts.intEnderecoInvalido); // se nao for liga interrupcao no meio da exec da
                                                                      // instrucao
            return false;
        }
    }

    private boolean testOverflow(int v) { // toda operacao matematica deve avaliar se ocorre overflow
        if ((v < minInt) || (v > maxInt)) {
            GlobalVariables.irpt.add(Interrupts.intOverflow); // se houver liga interrupcao no meio da exec da instrucao
            return false;
        }
        ;
        return true;
    }

    public void setContext(int _pc, int[] _reg) { // usado para setar o contexto da cpu para rodar um processo
        reg = _reg;
        pc = _pc; // pc cfe endereco logico
    }

    public int mmu(int pc) {
        int page = pc / 4;

        if (!GlobalVariables.running.getPagesTable()[page].isValid()) {
            System.out.println("Pagina " + page + " não válida. PAGE FAULT!!!!");
            GlobalVariables.irpt.add(Interrupts.pageFault);
            return -1;
        }

        int block = GlobalVariables.running.getPagesTable()[page].getFrame();
        return 4 * block + (pc % 4);
    }

    public void run() throws InterruptedException { // execucao da CPU supoe que o contexto da CPU, vide acima,
        // cpuStop = false;
        while (!GlobalVariables.shutdown) { // ciclo de instrucoes. acaba cfe resultado da exec da instrucao, veja cada
            // caso.
            // Espera o scheduler liberar a CPU para o processo
            GlobalVariables.semaphoreCPU.acquire();
            boolean processEnd = false;

            System.out.println("\n Rodando processo: " + GlobalVariables.running.getId());

            // RoundRobin
            for (int j = 0; j < Q; j++) {

                // --------------------------------------------------------------------------------------------------
                // FASE DE FETCH
                int physPC = mmu(pc); // mmu faz a traducao de endereco logico para fisico, se necessario
                System.out.println(
                        "\nExec j=" + j + " pc(log)=" + pc + " pc(phy)=" + physPC + " irpt=" + GlobalVariables.irpt);

                if (physPC != -1) {
                    if (legal(physPC)) { // pc valido
                        ir = m[physPC]; // <<<<<<<<<<<< AQUI faz FETCH - busca posicao da memoria apontada por pc,
                                        // guarda em ir
                        // resto é dump de debug
                        if (debug) {
                            System.out.print("                                              regs: ");
                            for (int i = 0; i < 10; i++) {
                                System.out.print(" r[" + i + "]:" + reg[i]);
                            }
                            ;
                            System.out.println();
                        }
                        if (debug) {
                            System.out.print("                      pc: " + pc + "       exec: ");
                            u.dump(ir);
                        }

                        // --------------------------------------------------------------------------------------------------
                        // FASE DE EXECUCAO DA INSTRUCAO CARREGADA NO ir
                        switch (ir.opc) { // conforme o opcode (código de operação) executa

                            // Instrucoes de Busca e Armazenamento em Memoria
                            case LDI: // Rd ← k veja a tabela de instrucoes do HW simulado para entender a semantica
                                      // da instrucao
                                reg[ir.ra] = ir.p;
                                pc++;
                                break;
                            case LDD: // Rd <- [A]
                                int phys1 = mmu(ir.p);
                                if (legal(phys1)) {
                                    reg[ir.ra] = m[phys1].p;
                                    pc++;
                                }
                                break;
                            case LDX: // RD <- [RS] // NOVA
                                int phys2 = mmu(reg[ir.rb]);
                                if (legal(phys2)) {
                                    reg[ir.ra] = m[phys2].p;
                                    pc++;
                                }
                                break;
                            case STD: // [A] ← Rs
                                int phys3 = mmu(ir.p);
                                if (legal(phys3)) {
                                    m[phys3].opc = Opcode.DATA;
                                    m[phys3].p = reg[ir.ra];
                                    pc++;
                                    if (debug) {
                                        System.out.print("                                  ");
                                        u.dump(phys3, phys3 + 1);
                                    }
                                }
                                break;
                            case STX: // [Rd] ←Rs
                                int phys4 = mmu(reg[ir.ra]);
                                if (legal(phys4)) {
                                    m[phys4].opc = Opcode.DATA;
                                    m[phys4].p = reg[ir.rb];
                                    pc++;
                                }
                                ;
                                break;
                            case MOVE: // RD <- RS
                                reg[ir.ra] = reg[ir.rb];
                                pc++;
                                break;
                            // Instrucoes Aritmeticas
                            case ADD: // Rd ← Rd + Rs
                                reg[ir.ra] = reg[ir.ra] + reg[ir.rb];
                                testOverflow(reg[ir.ra]);
                                pc++;
                                break;
                            case ADDI: // Rd ← Rd + k
                                reg[ir.ra] = reg[ir.ra] + ir.p;
                                testOverflow(reg[ir.ra]);
                                pc++;
                                break;
                            case SUB: // Rd ← Rd - Rs
                                reg[ir.ra] = reg[ir.ra] - reg[ir.rb];
                                testOverflow(reg[ir.ra]);
                                pc++;
                                break;
                            case SUBI: // RD <- RD - k // NOVA
                                reg[ir.ra] = reg[ir.ra] - ir.p;
                                testOverflow(reg[ir.ra]);
                                pc++;
                                break;
                            case MULT: // Rd <- Rd * Rs
                                reg[ir.ra] = reg[ir.ra] * reg[ir.rb];
                                testOverflow(reg[ir.ra]);
                                pc++;
                                break;

                            // Instrucoes JUMP
                            case JMP: // PC <- k
                                pc = ir.p;
                                break;
                            case JMPIM: // PC <- [A]
                                int phys5 = mmu(ir.p);
                                pc = m[phys5].p;
                                break;
                            case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
                                if (reg[ir.rb] > 0) {
                                    pc = reg[ir.ra];
                                } else {
                                    pc++;
                                }
                                break;
                            case JMPIGK: // If RC > 0 then PC <- k else PC++
                                if (reg[ir.rb] > 0) {
                                    pc = ir.p;
                                } else {
                                    pc++;
                                }
                                break;
                            case JMPILK: // If RC < 0 then PC <- k else PC++
                                if (reg[ir.rb] < 0) {
                                    pc = ir.p;
                                } else {
                                    pc++;
                                }
                                break;
                            case JMPIEK: // If RC = 0 then PC <- k else PC++
                                if (reg[ir.rb] == 0) {
                                    pc = ir.p;
                                } else {
                                    pc++;
                                }
                                break;
                            case JMPIL: // if Rc < 0 then PC <- Rs Else PC <- PC +1
                                if (reg[ir.rb] < 0) {
                                    pc = reg[ir.ra];
                                } else {
                                    pc++;
                                }
                                break;
                            case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1
                                if (reg[ir.rb] == 0) {
                                    pc = reg[ir.ra];
                                } else {
                                    pc++;
                                }
                                break;
                            case JMPIGM: // If RC > 0 then PC <- [A] else PC++
                                int phys6 = mmu(ir.p);
                                if (legal(phys6)) {
                                    if (reg[ir.rb] > 0) {
                                        pc = m[phys6].p;
                                    } else {
                                        pc++;
                                    }
                                }
                                break;
                            case JMPILM: // If RC < 0 then PC <- k else PC++
                                int phys7 = mmu(ir.p);
                                if (reg[ir.rb] < 0) {
                                    pc = m[phys7].p;
                                } else {
                                    pc++;
                                }
                                break;
                            case JMPIEM: // If RC = 0 then PC <- k else PC++
                                int phys8 = mmu(ir.p);
                                if (reg[ir.rb] == 0) {
                                    pc = m[phys8].p;
                                } else {
                                    pc++;
                                }
                                break;
                            case JMPIGT: // If RS>RC then PC <- k else PC++
                                if (reg[ir.ra] > reg[ir.rb]) {
                                    pc = ir.p;
                                } else {
                                    pc++;
                                }
                                break;

                            case DATA: // pc está sobre área supostamente de dados
                                GlobalVariables.irpt.add(Interrupts.intInstrucaoInvalida);
                                break;

                            // Chamadas de sistema
                            case SYSCALL:
                                GlobalVariables.sysc.add(SysCalls.ioRequest); // <<<<< aqui desvia para rotina de
                                                                              // chamada de
                                // sistema, no momento so
                                pc++;
                                break;

                            case STOP: // por enquanto, para execucao
                                processEnd = true;
                                break;

                            // Inexistente
                            default:
                                GlobalVariables.irpt.add(Interrupts.intInstrucaoInvalida);
                                break;
                        }
                    }
                }

                if (processEnd)
                    GlobalVariables.sysc.add(SysCalls.processEnd);
                else if (j == Q - 1)
                    GlobalVariables.irpt.add(Interrupts.roundRobin);

                // --------------------------------------------------------------------------------------------------
                // VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
                if (!GlobalVariables.irpt.isEmpty()) {
                    for (Interrupts i : GlobalVariables.irpt) {
                        ih.handle(i); // desvia para rotina de tratamento - esta rotina é do SO
                        GlobalVariables.irpt.poll();
                        break;
                    }
                }

                if (!GlobalVariables.sysc.isEmpty()) {
                    for (SysCalls s : GlobalVariables.sysc) {
                        sysCall.handle(s); // desvia para rotina de tratamento - esta rotina é do SO
                        GlobalVariables.sysc.poll();
                        j = Q;
                        break;
                    }
                }
            }

        } // FIM DO CICLO DE UMA INSTRUÇÃO
    }
}
