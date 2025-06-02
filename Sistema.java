import java.util.*;
import java.util.concurrent.Semaphore;

public class Sistema {
	private volatile boolean shutdown = false;
	private final List<Thread> workerThreads = new ArrayList<>();

	private static boolean ioRequest = false;

	Semaphore semaphoreCPU = new Semaphore(0); 
	Semaphore semaphoreScheduler = new Semaphore(0);
	Semaphore semaphoreConsole = new Semaphore(0);

	Queue<Interrupts> irpt = new LinkedList<>();
	Queue<SysCalls> sysc = new LinkedList<>();

	// -------------------------------------------------------------------------------------------------------
	// --------------------- T H R E A D S - definicoes de threads

	public class CpuRunnable implements Runnable {
		private CPU cpu;

		public CpuRunnable(CPU cpu) {
			this.cpu = cpu;
		}

		public void run() {
			System.out.println("Thread CPU em execução.");
			try {
				cpu.run();
			} catch (InterruptedException e) {
				// e.printStackTrace();
			}
		}
	}

	public class SchedulerRunning implements Runnable {
		private Scheduler scheduler;

		public SchedulerRunning(Scheduler scheduler) {
			this.scheduler = scheduler;
		}

		public void run() {
			System.out.println("Thread Scheduler em execução.");
			try {
				scheduler.roundRobin();
			} catch (InterruptedException e) {
				// e.printStackTrace();
			}
		}
	}

	public class ConsoleRunnable implements Runnable {
		private Console console;

		public ConsoleRunnable(Console console) {
			this.console = console;
		}

		public void run() {
			System.out.println("Thread Console em execução.");
			try {
				console.run();
			} catch (InterruptedException e) {
				// e.printStackTrace();
			}
		}
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------------- H A R D W A R E - definicoes de HW
	// ----------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// --------------------- M E M O R I A - definicoes de palavra de memoria,
	// memória ----------------------

	public class Memory {
		public Word[] pos; // pos[i] é a posição i da memória. cada posição é uma palavra.

		public Memory(int size) {
			pos = new Word[size];
			for (int i = 0; i < pos.length; i++) {
				pos[i] = new Word(Opcode.___, -1, -1, -1);
			} // cada posicao da memoria inicializada
		}
	}

	public class Disk {
		public Word[] pos;

		public Disk(int size) {
			pos = new Word[size];
			for (int i = 0; i < pos.length; i++) {
				pos[i] = new Word(Opcode.___, -1, -1, -1);
			}
		}
	}

	public class Word {    // cada posicao da memoria tem uma instrucao (ou um dado)
		public Opcode opc; //
		public int ra;     // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
		public int rb;     // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
		public int p;      // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

		public Word(Opcode _opc, int _ra, int _rb, int _p) { // vide definição da VM - colunas vermelhas da tabela
			opc = _opc;
			ra = _ra;
			rb = _rb;
			p  = _p;
		}
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------------- C P U - definicoes da CPU
	// -----------------------------------------------------

	public enum Opcode {
		DATA, ___,                      // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
		JMP, JMPI, JMPIG, JMPIL, JMPIE, // desvios
		JMPIM, JMPIGM, JMPILM, JMPIEM,
		JMPIGK, JMPILK, JMPIEK, JMPIGT,
		ADDI, SUBI, ADD, SUB, MULT,    // matematicos
		LDI, LDD, STD, LDX, STX, MOVE, // movimentacao
		SYSCALL, STOP                  // chamada de sistema e parada
	}

	public enum Interrupts {           // possiveis interrupcoes que esta CPU gera
		intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intSTOP, roundRobin, ioFinished, pageFault, pageSaved, pageLoaded;
	}

	public enum SysCalls {
		ioRequest, processEnd
	}

	public enum ProcessStates {
		blocked, running, ready, finished
	}

	public int mmu(int pc) {
		int page = pc / 4;

		if (!running.getPagesTable()[page].isValid()) {
			System.out.println("Pagina " + page + " não válida. PAGE FAULT!!!!");
			irpt.add(Interrupts.pageFault);
			return -1;
		}

		int block = running.getPagesTable()[page].getFrame();
		return 4 * block + (pc % 4);
	}

	public class CPU {
		private static final int Q = 4;
		private int maxInt; // valores maximo e minimo para inteiros nesta cpu
		private int minInt;
		                    // CONTEXTO da CPU ...
		private int pc;     // ... composto de program counter,
		private Word ir;    // instruction register,
		private int[] reg;  // registradores da CPU
		                    // FIM CONTEXTO DA CPU: tudo que precisa sobre o estado de um processo para
		                    // executa-lo
		                    // nas proximas versoes isto pode modificar

		private Word[] m;   // m é o array de memória "física", CPU tem uma ref a m para acessar
		private Word[] d;   // d é o array de disco "físico", CPU tem uma ref a d para acessar

		private InterruptHandling ih;    // significa desvio para rotinas de tratamento de Int - se int ligada, desvia
		private SysCallHandling sysCall; // significa desvio para tratamento de chamadas de sistema

		                            // auxilio aa depuração
		private boolean debug;      // se true entao mostra cada instrucao em execucao
		private Utilities u;        // para debug (dump)

		public CPU(Memory _mem, Disk _disk, boolean _debug) { // ref a MEMORIA passada na criacao da CPU
			maxInt = 32767;            // capacidade de representacao modelada
			minInt = -32767;           // se exceder deve gerar interrupcao de overflow
			m = _mem.pos;              // usa o atributo 'm' para acessar a memoria, só para ficar mais pratico
			d = _disk.pos;             // usa o atributo 'd' para acessar o disco, só para ficar mais pratico
			reg = new int[10];         // aloca o espaço dos registradores - regs 8 e 9 usados somente para IO

			debug = _debug;            // se true, print da instrucao em execucao

		}

		public void setAddressOfHandlers(InterruptHandling _ih, SysCallHandling _sysCall) {
			ih = _ih;                  // aponta para rotinas de tratamento de int
			sysCall = _sysCall;        // aponta para rotinas de tratamento de chamadas de sistema
		}

		public void setUtilities(Utilities _u) {
			u = _u;                     // aponta para rotinas utilitárias - fazer dump da memória na tela
		}


                                       // verificação de enderecamento 
		private boolean legal(int e) { // todo acesso a memoria tem que ser verificado se é válido - 
			                           // aqui no caso se o endereco é um endereco valido em toda memoria
			if (e >= 0 && e < m.length) {
				return true;
			} else {
				irpt.add(Interrupts.intEnderecoInvalido);    // se nao for liga interrupcao no meio da exec da instrucao
				return false;
			}
		}

		private boolean testOverflow(int v) {             // toda operacao matematica deve avaliar se ocorre overflow
			if ((v < minInt) || (v > maxInt)) {
				irpt.add(Interrupts.intOverflow);            // se houver liga interrupcao no meio da exec da instrucao
				return false;
			}
			;
			return true;
		}

		public void setContext(int _pc, int[] _reg) {                 // usado para setar o contexto da cpu para rodar um processo
			reg = _reg;                                   
			pc = _pc;                                     // pc cfe endereco logico         
		}

		public void run() throws InterruptedException {                               // execucao da CPU supoe que o contexto da CPU, vide acima, 
			// cpuStop = false;
			while (!shutdown) {      // ciclo de instrucoes. acaba cfe resultado da exec da instrucao, veja cada caso.
				// Espera o scheduler liberar a CPU para o processo
				semaphoreCPU.acquire();
				boolean processEnd = false;
				
				System.out.println("\n Rodando processo: " + running.getId());

				// RoundRobin
				for(int j = 0; j < Q; j++) {
					
					// --------------------------------------------------------------------------------------------------
					// FASE DE FETCH
					int physPC = mmu(pc); // mmu faz a traducao de endereco logico para fisico, se necessario
					System.out.println("\nExec j=" + j + " pc(log)=" + pc + " pc(phy)=" + physPC + " irpt=" + irpt);

					if (physPC != -1 && legal(physPC)) { // pc valido
						ir = m[physPC];  // <<<<<<<<<<<< AQUI faz FETCH - busca posicao da memoria apontada por pc, guarda em ir
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
						switch (ir.opc) {       // conforme o opcode (código de operação) executa

							// Instrucoes de Busca e Armazenamento em Memoria
							case LDI: // Rd ← k        veja a tabela de instrucoes do HW simulado para entender a semantica da instrucao
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
									if (debug) 
										{   System.out.print("                                  ");   
											u.dump(phys3,phys3+1);							
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
								if (legal(phys6)){
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
								irpt.add(Interrupts.intInstrucaoInvalida);
								break;

							// Chamadas de sistema
							case SYSCALL:
								sysc.add(SysCalls.ioRequest); // <<<<< aqui desvia para rotina de chamada de sistema, no momento so
								pc++;
								break;

							case STOP: // por enquanto, para execucao
								processEnd = true;
								break;

							// Inexistente
							default:
								irpt.add(Interrupts.intInstrucaoInvalida);
								break;
						}
					}

					if (processEnd) sysc.add(SysCalls.processEnd);
					else if (j == Q - 1) irpt.add(Interrupts.roundRobin);

					// --------------------------------------------------------------------------------------------------
					// VERIFICA INTERRUPÇÃO !!! - TERCEIRA FASE DO CICLO DE INSTRUÇÕES
					if (!irpt.isEmpty()) {
						for (Interrupts i : irpt) {
							ih.handle(i);                  // desvia para rotina de tratamento - esta rotina é do SO
							irpt.poll();
							break;
						}
					}

					if (!sysc.isEmpty()) {
						for (SysCalls s : sysc) {
							sysCall.handle(s);             // desvia para rotina de tratamento - esta rotina é do SO
							sysc.poll();
							j = Q;
							break;
						}
					}
				}

				
			} // FIM DO CICLO DE UMA INSTRUÇÃO
		}
	}
	// ------------------ C P U - fim
	// -----------------------------------------------------------------------
	// ------------------------------------------------------------------------------------------------------

	// ------------------- HW - constituido de CPU e MEMORIA
	// -----------------------------------------------
	public class HW {
		public Memory mem;
		public Disk disk;
		public CPU cpu;

		public HW(int tamMem) {
			mem = new Memory(tamMem);
			disk = new Disk(4096);
			cpu = new CPU(mem, disk, true); // true liga debug

			CpuRunnable cpuRunnable = new CpuRunnable(cpu);
			Thread cpuThread = new Thread(cpuRunnable);
			workerThreads.add(cpuThread);
			cpuThread.start();
		}
	}
	// -------------------------------------------------------------------------------------------------------

	// --------------------H A R D W A R E - fim
	// -------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////

	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// ------------------- SW - inicio - Sistema Operacional
	// -------------------------------------------------

	// ------------------- I N T E R R U P C O E S - rotinas de tratamento
	// ----------------------------------
	public class InterruptHandling {
		private HW hw; // referencia ao hw se tiver que setar algo
		private ProcessManager pm;

		public InterruptHandling(HW _hw, ProcessManager _pm) {
			hw = _hw;
			pm = _pm;
		}

		public void handle(Interrupts irpt) {
			System.out.println(
					"                                               Interrupcao " + irpt + "   pc: " + hw.cpu.pc);

			switch(irpt) {
				case roundRobin:
					System.out.println("RoundRobin");
					semaphoreScheduler.release();
					break;
				case intInstrucaoInvalida:
					System.out.println("Instrucao invalida - A execucao do processo " + running.getId() + " sera pausada e o processo cancelado.");

					// Se o processo tem uma instrucao invalida ele e desalocado da memoria e retirado da fila de processos
					pm.dealloc(running.getId());
					running.setStates(ProcessStates.finished);

					semaphoreScheduler.release();
					break;
				case ioFinished:
					System.out.println("IO Finished");

					PCB p = blocked.poll();
					ready.add(p);
					p.setStates(ProcessStates.ready);

					break;
				case intEnderecoInvalido:
					// System.out.println("Endereco invalido - A execucao do processo " + running.getId() + " sera pausada e o processo cancelado.");

					// pm.dealloc(running.getId());
					// running.setStates(ProcessStates.finished);

					// semaphoreScheduler.release();
					break;
				case pageFault:
					System.out.println("Page Fault - A pagina nao esta carregada na memoria");

					ready.remove(running);
					blocked.add(running);
					
					running.setStates(ProcessStates.blocked);
					running.setContext(hw.cpu.pc, hw.cpu.reg);
				
					// Aqui deve-se carregar a pagina na memoria, se nao tiver espaço deve-se desalocar uma pagina
					pm.allocPageFault(running.id);

					semaphoreScheduler.release();

					break;
				default:
					System.out.println("Interrupção não tratada: " + irpt);
					break;
			}
		}
	}
	//TODO -> console e chamado pela syscall e deve passar os registradores para salvar na memoria (deve ser o fisico nao o logico)

	// ------------------- C H A M A D A S D E S I S T E M A - rotinas de tratamento
	// ----------------------
	public class SysCallHandling {
		private HW hw; // referencia ao hw se tiver que setar algo
		private ProcessManager pm;

		public SysCallHandling(HW _hw, ProcessManager _pm) {
			hw = _hw;
			pm = _pm;
		}

		public void handle(SysCalls sysc) { // chamada de sistema - suporta somente IO, com parametros - reg[8] = in ou out    e reg[9] endereco do inteiro
			System.out.println("SYSCALL pars:  " + hw.cpu.reg[8] + " / " + hw.cpu.reg[9]);
			System.out.println("sysc: " + sysc);	

			switch (sysc){
				case ioRequest:
					System.out.println("IoRequest");
					ready.remove(running);
					blocked.add(running);
					
					running.setStates(ProcessStates.blocked);
					running.setContext(hw.cpu.pc, hw.cpu.reg);
				
					switch(hw.cpu.reg[8]) {
						case 1:
							ioRequest = true;
							semaphoreConsole.release();
							break;
						case 2:
							// escrita - escreve o conteuodo da memoria na posicao dada em reg[9]
							ioRequest = false;
							semaphoreConsole.release();
							break;
						default:
							System.out.println("  PARAMETRO INVALIDO");	
							break;
					}

					semaphoreScheduler.release();
					break;
				case processEnd:
					System.out.println("ProcessEnd");

					if (running == null) return;

					pm.dealloc(running.getId());
					running.setStates(ProcessStates.finished);

					semaphoreScheduler.release();
					break;
				default:
					System.out.println("Syscall nao tratada");
					break;
			}
		}
	}

	// ------------------ U T I L I T A R I O S D O S I S T E M A
	// -----------------------------------------
	// ------------------ load é invocado a partir de requisição do usuário

	// carga na memória
	public class Utilities {
		private HW hw;

		public Utilities(HW _hw) {
			hw = _hw;
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
			Word[] m = hw.mem.pos; // m[] é o array de posições memória do hw
			for (int i = ini; i < fim; i++) {
				System.out.print(i);
				System.out.print(":  ");
				dump(m[i]);
			}
		}
	}

	public class SO {
		public InterruptHandling ih;
		public SysCallHandling sc;
		public Utilities utils;
		public MemoryManager mm;
		public ProcessManager pm;
		public PCB running;
		public Scheduler scheduler;
		public Console console;

		public SO(HW hw, int tamMem, int tamPag) {
			utils = new Utilities(hw);
			mm = new MemoryManager(tamMem, tamPag);
			pm = new ProcessManager(mm);
			scheduler = new Scheduler();
			console = new Console();
			ih = new InterruptHandling(hw, pm);
			sc = new SysCallHandling(hw,pm);
			hw.cpu.setAddressOfHandlers(ih, sc);

			SchedulerRunning schedulerRunning = new SchedulerRunning(scheduler);
			Thread schedulerThread = new Thread(schedulerRunning);

			ConsoleRunnable consoleRunnable = new ConsoleRunnable(console);
			Thread consoleThread = new Thread(consoleRunnable);

			workerThreads.add(schedulerThread);
			workerThreads.add(consoleThread);
			schedulerThread.start();
			consoleThread.start();
		}

		// cria um processo na memória
		public boolean newProcess(Word[] program) {
			running = pm.createProcess(program);

			return running == null;
		}

		// lista todos processos existentes
		public void ps() {
			for (PCB pcb : pm.getProcesses()) {
				System.out.println(pcb);
			}
		}

		public void execAll() {
			if (ready.isEmpty()) {
				System.out.println("Não há processos prontos para execução.");
				return;
			}

			semaphoreScheduler.release();
		}

		// retira o processo id do sistema, tenha ele executado ou não
		public boolean rmProcess(int id) {
			if (id < 0) {
				System.out.println("ID inválido.");
				return false;
			}

			pm.dealloc(id);
			return true;
		}

		// lista o conteúdo do PCB e o conteúdo da memória do processo com id
		public void dump(int id) {
			PCB target = null;

			for (PCB pcb : pm.getProcesses()) {
				if (pcb.getId() == id) {
					target = pcb;
					break;
				}
			}

			if (target != null) {
				PageTableEntry[] pages = target.getPagesTable();
				int pageSize = mm.getPageSize();

				System.out.println("Dump | id " + id + ":");

				for (int i = 0; i < pages.length; i++) {
					int start = pages[i].getFrame() * pageSize;
					int end = start + pageSize;
					utils.dump(start, end);
				}
			}
		}

		// Lista a memória entre posições início e fim, independente do processo
		public void dumpM(int start, int end) {
			utils.dump(start, end);
		}

		// Liga modo de execução em que CPU print cada instrução executada
		public void traceOn() { 
			hw.cpu.debug = true;
		}

		// Desliga o modo acima
		public void traceOff() {
			hw.cpu.debug = false;
		}

		// public void exit() {} do sistema operacional
	}

	// -------------------------------------------------------------------------------------------------------
	// ------------------- instancia e testa sistema
	public static void main(String[] args) {
		Sistema sistema = new Sistema(1024, 4);
		sistema.run();
	}

	// -------------------------------------------------------------------------------------------------------
	// ------------------- S I S T E M A
	// --------------------------------------------------------------------

	public HW hw;
	public SO so;
	public Programs progs;
	public static Queue<PCB> ready;
	public static Queue<PCB> blocked;
	public PCB running;
	public boolean autoMode = true;

	public Sistema(int tamMem, int tamPag) {
		hw = new HW(tamMem);           // memoria do HW tem tamMem palavras
		so = new SO(hw, tamMem, tamPag);
		ready = new LinkedList<>();
		blocked = new LinkedList<>();

		hw.cpu.setUtilities(so.utils); // permite cpu fazer dump de memoria ao avancar
		progs = new Programs();
	}

	public void run() {
		System.out.println("\n\n### ### ### SISTEMA OPERACIONAL ### ### ###");

		Scanner in = new Scanner(System.in);
		int op;
		do {
			System.out.println("\n\nOPERAÇÕES");
			System.out.println("1 - Criar processo");
			System.out.println("2 - Listar processos");
			System.out.println("3 - Remover processo por ID");
			System.out.println("4 - Executar processos");
			System.out.println("5 - Dump de processo");
			System.out.println("6 - Dump de memória");
			System.out.println("7 - Trace on");
			System.out.println("8 - Trace off");
			System.out.println("9 - Toggle autoMode");
			System.out.println("0 - Sair");
			System.out.print("> Informe a operação que deseja realizar: ");
			op = in.nextInt();

			switch (op) {
				case 1:
					System.out.println("\n\nPROGRAMAS");
					System.out.println("1 - fatorial");
					System.out.println("2 - fatorialV2");
					System.out.println("3 - progMinimo");
					System.out.println("4 - fibonacci10");
					System.out.println("5 - fibonacci10v2");
					System.out.println("6 - PB");
					System.out.println("7 - PC");
					System.out.println("0 - Voltar");
					System.out.print("> Informe o programa que deseja executar: ");
					int program = in.nextInt();

					switch (program) {
						case 1:
							so.newProcess(progs.retrieveProgram("fatorial"));
							break;

						case 2:
							so.newProcess(progs.retrieveProgram("fatorialV2"));
							break;
						
						case 3:
							so.newProcess(progs.retrieveProgram("progMinimo"));
							break;

						case 4:
							so.newProcess(progs.retrieveProgram("fibonacci10"));
							break;

						case 5:
							so.newProcess(progs.retrieveProgram("fibonacci10v2"));
							break;

						case 6:
							so.newProcess(progs.retrieveProgram("PB"));
							break;

						case 7:
							so.newProcess(progs.retrieveProgram("PC"));
							break;

						case 0:
							System.out.println("Retornando ao menu de operações do sistema operacional...");
							break;

						default:
							System.out.println("ERRO: Operação inválida. Tente novamente");
							break;
					}
					break;
				
				
				case 2:
					System.out.println("Listando processos...");
					so.ps();
					break;

				case 3:
					System.out.println("\n\nPROCESSOS");
					so.ps();
					System.out.print("> Informe o processo que deseja remover: ");
					int processId = in.nextInt();

					if (so.rmProcess(processId)) {
						System.out.println("Processo " + processId + " removido com sucesso.");
					} else {
						System.out.println("ERRO: Processo não encontrado.");
					}

					break;

				case 4:
					if (autoMode) return;
					System.out.println("Executando todos os processos...");
					so.execAll();
					break;
				
				case 5:
					System.out.println("Dump de processo por ID...");
					System.out.print("> Informe o processo que deseja realizar dump: ");
					processId = in.nextInt();

					if (processId < 0) {
						System.out.println("ID inválido.");
						break;
					}

					so.dump(processId);
					break;

				case 6:
					System.out.println("Dump de memória...");
					System.out.print("> Informe o início do dump de memória: ");
					int dumpStart = in.nextInt();
					System.out.print("> Informe o fim do dump de memória: ");
					int dumpEnd = in.nextInt();

					if (dumpStart < 0 || dumpEnd < 0 || dumpStart >= hw.mem.pos.length || dumpEnd >= hw.mem.pos.length || dumpStart > dumpEnd) {
						System.out.println("ERRO: Intervalo inválido.");
						break;
					}

					so.dumpM(dumpStart, dumpEnd);
					break;

				case 7:
					System.out.println("Trace on...");
					so.traceOn();
					break;
					
				case 8:
					System.out.println("Trace off...");
					so.traceOff();
					break;

				case 9:
					autoMode = !autoMode;
					System.out.println("Modo automático: " + (autoMode ? "Ativado" : "Desativado"));
					break;

				case 0:
					System.out.println("Encerrando sistema operacional...");
					shutdown = true;

					for (Thread t : workerThreads) {
						t.interrupt();
					}
					
					for (Thread t : workerThreads) {
						try {
							t.join();
						} catch (InterruptedException e) { /* ignore */ }
					}
					
					System.out.println("Todas as threads encerradas.");
					break;

				default:
					System.out.println("ERRO: Operação inválida. Tente novamente");
					break;
			}

		} while (op != 0);
		in.close();
	}

	// Gerenciador de memória

	public class MemoryManager {
		private int pageSize;
		private int framesNumber;
		private boolean[] allocatedFrames;
		private Queue<Integer> frameQueue = new LinkedList<>();

		public MemoryManager(int memorySize, int pageSize) {
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

		public int requestFrame(PCB requestingProcess, int pageIndex, List<PCB> processes) {
			for (int i = 0; i < framesNumber; i++) {
				if (!allocatedFrames[i]) {
					allocatedFrames[i] = true;
					frameQueue.add(i);
					return i;
				}
			}

			int victimFrame = frameQueue.poll(); 
			allocatedFrames[victimFrame] = true;

			for (PCB pcb : processes) {
				for (PageTableEntry entry : pcb.getPagesTable()) {
					if (entry.isValid() && entry.getFrame() == victimFrame) {
						
						int page = Arrays.asList(pcb.getPagesTable()).indexOf(entry);
						int baseRam = victimFrame * pageSize;
						int baseDisk = page * pageSize;

						for (int i = 0; i < pageSize; i++) {
							hw.disk.pos[baseDisk + i] = hw.mem.pos[baseRam + i];
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

	public class ProcessManager {
		private MemoryManager mm;
		private List<PCB> processes;

		public ProcessManager(MemoryManager mm) {
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
						if (w.p > maxLogicalAddr) maxLogicalAddr = w.p;
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

			System.out.println("PC: " + pc + " | PageSize: " + pageSize + " | PagesTable: " + Arrays.toString(pageTable));
			
			int logicalAddr = 0;
			for (Word w : program) {
				int page  = logicalAddr / pageSize;
				int offset = logicalAddr % pageSize;
				int physDisk = page * pageSize + offset;

				// Salva código completo no disco
				hw.disk.pos[physDisk].opc = w.opc;
				hw.disk.pos[physDisk].ra  = w.ra;
				hw.disk.pos[physDisk].rb  = w.rb;
				hw.disk.pos[physDisk].p   = w.p;

				// Se a página estiver na RAM (somente a 0 no início), também copia para a RAM
				if (pageTable[page].isValid()) {
					int frame = pageTable[page].getFrame();
					int physRam = frame * pageSize + offset;

					hw.mem.pos[physRam].opc = w.opc;
					hw.mem.pos[physRam].ra  = w.ra;
					hw.mem.pos[physRam].rb  = w.rb;
					hw.mem.pos[physRam].p   = w.p;
				}

				logicalAddr++;
			}

			processes.add(pcb);
			ready.add(pcb);
			pcb.setStates(ProcessStates.ready);
			if (autoMode) semaphoreScheduler.release();

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
			ready.remove(target);
			target.setStates(ProcessStates.finished);
		}

		public void allocPageFault(int id) {
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
				return;
			}

			int pageSize = mm.getPageSize();
			int pc = target.getPc();
			int pageIndex = pc / pageSize;

			System.out.println("PAGE FAULT no processo " + id + ", página " + pageIndex);

			int frame = mm.requestFrame(target, pageIndex, processes);
			target.getPagesTable()[pageIndex].setValid(true);
			target.getPagesTable()[pageIndex].setFrame(frame);

			// Copiar do disco para a RAM (swap in)
			int baseDisk = pageIndex * pageSize;
			int baseRam = frame * pageSize;

			for (int i = 0; i < pageSize; i++) {
				hw.mem.pos[baseRam + i] = hw.disk.pos[baseDisk + i];
			}

			// Bloqueia o processo
			target.setStates(ProcessStates.ready);

			System.out.println("Página " + pageIndex + " carregada para o frame " + frame);
		}
	}

	public class PCB {
		private static int idCounter;
		private int id;
		private int pc;
		private int[] regState;
		private PageTableEntry[] pagesTable;
		private ProcessStates state;

		public PCB(int pc, PageTableEntry[] pagesTable) {
			idCounter++;
			this.id = idCounter;
			this.pc = pc;
			this.pagesTable = pagesTable;
			this.regState = new int[10];
		}

        public int getId() { return this.id; }
        public int getPc() { return this.pc; }
		public int[] getRegState() { return this.regState; }
		public PageTableEntry[] getPagesTable() { return pagesTable; }
        public void setStates(ProcessStates ps) { this.state = ps; }
		public ProcessStates getState() { return this.state; }

		public void setContext(int pc, int[] reg) {
			this.pc = pc;
			this.regState = reg;
		}

		public void setRegState(int reg, int value) {
			this.regState[reg] = value;
		}

		@Override
		public String toString() {
			return "PCB [id=" + id + ", pc=" + pc + ", regState=" + Arrays.toString(regState) + ", pagesTable="
					+ Arrays.toString(pagesTable) + ", state=" + state + "]";
		}
	}

	public class PageTableEntry {
		private boolean isValid;
		private int frame;

		public PageTableEntry() {
			this.isValid = false;
			this.frame = -1;
		}

		public boolean isValid() {
			return isValid;
		}
		public void setValid(boolean isValid) {
			this.isValid = isValid;
		}
		public int getFrame() {
			return frame;
		}
		public void setFrame(int frame) {
			this.frame = frame;
		}

		@Override
		public String toString() {
			return "PageTableEntry [isValid=" + isValid + ", frame=" + frame + "]";
		}
	}

	public class Scheduler {
		public void roundRobin() throws InterruptedException {
			while (!shutdown) {
				semaphoreScheduler.acquire();

				if (running != null && running.getState() != ProcessStates.finished) {
					running.setContext(hw.cpu.pc, hw.cpu.reg);

					running.setStates(ProcessStates.ready);

					ready.add(running);
				}

				if (ready.isEmpty()) {
					System.out.println("\nSem processos prontos para execução.");
					running = null;
					continue;
				}

				running = ready.poll();
				running.setStates(ProcessStates.running);
				hw.cpu.setContext(running.getPc(), running.getRegState());

				semaphoreCPU.release();
			}
		}
	}
	
	public class Console {
		public void run() throws InterruptedException {
			while (!shutdown) {
				Scanner in = new Scanner(System.in);

				semaphoreConsole.acquire();
				System.out.println("------------------------------------- [ CONSOLE ] -------------------------------------");
				
				// TODO -> Isso aqui deveria ser feito pelo DMA
				if (ioRequest) {
					System.out.println("Console: IO leitura");

					System.out.println("IN:   ");
					int input = in.nextInt();

					PCB p = blocked.peek();
					p.setRegState(9, input);

					in.close();
				} else {
					System.out.println("Console: IO escrita");

					PCB p = blocked.peek();

					System.out.println("OUT:   "+ p.getRegState()[9]);
				}

				irpt.add(Interrupts.ioFinished);
			}
		}
	}
	// ------------------- S I S T E M A - fim
	// --------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// --------------- P R O G R A M A S - não fazem parte do sistema
	// esta classe representa programas armazenados (como se estivessem em disco)
	// que podem ser carregados para a memória (load faz isto)

	public class Program {
		public String name;
		public Word[] image;

		public Program(String n, Word[] i) {
			name = n;
			image = i;
		}
	}

	public class Programs {

		public Word[] retrieveProgram(String pname) {
			for (Program p : progs) {
				if (p != null & p.name == pname)
					return p.image;
			}
			return null;
		}

		public Program[] progs = {
				new Program("fatorial",
						new Word[] {
								// este fatorial so aceita valores positivos. nao pode ser zero
								// linha coment
								new Word(Opcode.LDI, 0, -1, 7), // 0 r0 é valor a calcular fatorial
								new Word(Opcode.LDI, 1, -1, 1), // 1 r1 é 1 para multiplicar (por r0)
								new Word(Opcode.LDI, 6, -1, 1), // 2 r6 é 1 o decremento
								new Word(Opcode.LDI, 7, -1, 8), // 3 r7 tem posicao 8 para fim do programa
								new Word(Opcode.JMPIE, 7, 0, 0), // 4 se r0=0 pula para r7(=8)
								new Word(Opcode.MULT, 1, 0, -1), // 5 r1 = r1 * r0 (r1 acumula o produto por cada termo)
								new Word(Opcode.SUB, 0, 6, -1), // 6 r0 = r0 - r6 (r6=1) decrementa r0 para proximo
																// termo
								new Word(Opcode.JMP, -1, -1, 4), // 7 vai p posicao 4
								new Word(Opcode.STD, 1, -1, 10), // 8 coloca valor de r1 na posição 10
								new Word(Opcode.STOP, -1, -1, -1), // 9 stop
								new Word(Opcode.DATA, -1, -1, -1) // 10 ao final o valor está na posição 10 da memória
						}),

				new Program("fatorialV2",
						new Word[] {
								new Word(Opcode.LDI, 0, -1, 5), // numero para colocar na memoria, ou pode ser lido
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
								new Word(Opcode.LDI, 9, -1, 18), // endereco com valor a escrever
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
								new Word(Opcode.DATA, -1, -1, -1) // ate aqui - serie de fibonacci ficara armazenada
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
								new Word(Opcode.DATA, -1, -1, -1) // ate aqui - serie de fibonacci ficara armazenada
						}),
				new Program("fibonacciREAD",
						new Word[] {
								// mesmo que prog exemplo, so que usa r0 no lugar de r8
								new Word(Opcode.LDI, 8, -1, 1), // leitura
								new Word(Opcode.LDI, 9, -1, 55), // endereco a guardar o tamanho da serie de fib a gerar
																	// - pode ser de 1 a 20
								new Word(Opcode.SYSCALL, -1, -1, -1),
								new Word(Opcode.LDD, 7, -1, 55),
								new Word(Opcode.LDI, 3, -1, 0),
								new Word(Opcode.ADD, 3, 7, -1),
								new Word(Opcode.LDI, 4, -1, 36), // posicao para qual ira pular (stop) *
								new Word(Opcode.LDI, 1, -1, -1), // caso negativo
								new Word(Opcode.STD, 1, -1, 41),
								new Word(Opcode.JMPIL, 4, 7, -1), // pula pra stop caso negativo *
								new Word(Opcode.JMPIE, 4, 7, -1), // pula pra stop caso 0
								new Word(Opcode.ADDI, 7, -1, 41), // fibonacci + posição do stop
								new Word(Opcode.LDI, 1, -1, 0),
								new Word(Opcode.STD, 1, -1, 41), // 25 posicao de memoria onde inicia a serie de
																	// fibonacci gerada
								new Word(Opcode.SUBI, 3, -1, 1), // se 1 pula pro stop
								new Word(Opcode.JMPIE, 4, 3, -1),
								new Word(Opcode.ADDI, 3, -1, 1),
								new Word(Opcode.LDI, 2, -1, 1),
								new Word(Opcode.STD, 2, -1, 42),
								new Word(Opcode.SUBI, 3, -1, 2), // se 2 pula pro stop
								new Word(Opcode.JMPIE, 4, 3, -1),
								new Word(Opcode.LDI, 0, -1, 43),
								new Word(Opcode.LDI, 6, -1, 25), // salva posição de retorno do loop
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
								// se for negativo armazena -1 na saída; se for positivo responde o fatorial do
								// número na saída
								new Word(Opcode.LDI, 0, -1, 7), // numero para colocar na memoria
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
								// o programa ordena um vetor de N números em alguma posição de memória;
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
								new Word(Opcode.STD, 0, -1, 50), // colocando valores na memoria até aqui - POS 13
								new Word(Opcode.LDI, 3, -1, 25), // Posicao para pulo CHAVE 1
								new Word(Opcode.STD, 3, -1, 99),
								new Word(Opcode.LDI, 3, -1, 22), // Posicao para pulo CHAVE 2
								new Word(Opcode.STD, 3, -1, 98),
								new Word(Opcode.LDI, 3, -1, 38), // Posicao para pulo CHAVE 3
								new Word(Opcode.STD, 3, -1, 97),
								new Word(Opcode.LDI, 3, -1, 25), // Posicao para pulo CHAVE 4 (não usada)
								new Word(Opcode.STD, 3, -1, 96),
								new Word(Opcode.LDI, 6, -1, 0), // r6 = r7 - 1 POS 22
								new Word(Opcode.ADD, 6, 7, -1),
								new Word(Opcode.SUBI, 6, -1, 1), // ate aqui
								new Word(Opcode.JMPIEM, -1, 6, 97), // CHAVE 3 para pular quando r7 for 1 e r6 0 para
																	// interomper o loop de vez do programa
								new Word(Opcode.LDX, 0, 5, -1), // r0 e ra pegando valores das posições da memoria POS
																// 26
								new Word(Opcode.LDX, 1, 4, -1),
								new Word(Opcode.LDI, 2, -1, 0),
								new Word(Opcode.ADD, 2, 0, -1),
								new Word(Opcode.SUB, 2, 1, -1),
								new Word(Opcode.ADDI, 4, -1, 1),
								new Word(Opcode.SUBI, 6, -1, 1),
								new Word(Opcode.JMPILM, -1, 2, 99), // LOOP chave 1 caso neg procura prox
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
}