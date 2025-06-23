import java.util.*;

import hardware.HW;
import os.OS;
import utils.GlobalVariables;
import utils.Programs;

public class Sistema {
	// TODO: declarar processo nop - não faz nada
	// se não ha processo pronto, escalona um nop
	public HW hw;
	public OS os;
	public Programs progs;

	public Sistema(int memorySize, int diskSize, int pageSize) {
		hw = new HW(memorySize, diskSize); // memoria do HW tem tamMem palavras
		os = new OS(hw, memorySize, pageSize);

		hw.cpu.setUtilities(os.utils); // permite cpu fazer dump de memoria ao avancar
		progs = new Programs();
	}

	public static void main(String[] args) {
		int memorySize = 16;
		int diskSize = 4096;
		int pageSize = 4;

		Sistema sistema = new Sistema(memorySize, diskSize, pageSize);
		sistema.run();
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
			System.out.println("7 - Dump de disco");
			System.out.println("8 - Trace on");
			System.out.println("9 - Trace off");
			System.out.println(
					"10 - Alternar modo automático (atual: " + (GlobalVariables.autoMode ? "ativado" : "desativado")
							+ ")");
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
					System.out.println("6 - fibonacciREAD");
					System.out.println("7 - PB");
					System.out.println("8 - PC");
					System.out.println("0 - Voltar");
					System.out.print("> Informe o programa que deseja executar: ");
					int program = in.nextInt();

					switch (program) {
						case 1:
							os.newProcess(progs.retrieveProgram("fatorial"));
							break;

						case 2:
							os.newProcess(progs.retrieveProgram("fatorialV2"));
							break;

						case 3:
							os.newProcess(progs.retrieveProgram("progMinimo"));
							break;

						case 4:
							os.newProcess(progs.retrieveProgram("fibonacci10"));
							break;

						case 5:
							os.newProcess(progs.retrieveProgram("fibonacci10v2"));
							break;

						case 6:
							os.newProcess(progs.retrieveProgram("fibonacciREAD"));
							break;
							
						case 7:
							os.newProcess(progs.retrieveProgram("PB"));
							break;
							
						case 8:
							os.newProcess(progs.retrieveProgram("PC"));
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
					os.ps();
					break;

				case 3:
					System.out.println("\n\nPROCESSOS");
					os.ps();
					System.out.print("> Informe o processo que deseja remover: ");
					int processId = in.nextInt();

					if (os.rmProcess(processId)) {
						System.out.println("Processo " + processId + " removido com sucesos.");
					} else {
						System.out.println("ERRO: Processo não encontrado.");
					}

					break;

				case 4:
					if (GlobalVariables.autoMode)
						return;
					System.out.println("Executando todos os processos...");
					os.execAll();
					break;

				case 5:
					System.out.println("Dump de processo por ID...");
					System.out.print("> Informe o processo que deseja realizar dump: ");
					processId = in.nextInt();

					if (processId < 0) {
						System.out.println("ID inválido.");
						break;
					}

					os.dump(processId);
					break;

				case 6:
					System.out.println("Dump de memória...");
					System.out.print("> Informe o início do dump de memória: ");
					int dumpStart = in.nextInt();
					System.out.print("> Informe o fim do dump de memória: ");
					int dumpEnd = in.nextInt();

					if (dumpStart < 0 || dumpEnd < 0 || dumpStart >= hw.memory.pos.length
							|| dumpEnd >= hw.memory.pos.length
							|| dumpStart > dumpEnd) {
						System.out.println("ERRO: Intervalo inválido.");
						break;
					}

					os.dumpM(dumpStart, dumpEnd);
					break;

				case 7:
					System.out.println("Dump de disco...");
					System.out.print("> Informe o início do dump de disco: ");
					dumpStart = in.nextInt();
					System.out.print("> Informe o fim do dump de disco: ");
					dumpEnd = in.nextInt();

					if (dumpStart < 0 || dumpEnd < 0 || dumpStart >= hw.disk.pos.length
							|| dumpEnd >= hw.disk.pos.length
							|| dumpStart > dumpEnd) {
						System.out.println("ERRO: Intervalo inválido.");
						break;
					}
					os.dumpD(dumpStart, dumpEnd);
					break;

				case 8:
					System.out.println("Trace off...");
					os.traceOff();
					break;

				case 9:
					System.out.println("Trace on...");
					os.traceOn();
					break;

				case 10:
					GlobalVariables.autoMode = !GlobalVariables.autoMode;
					System.out.println("Modo automático: " + (GlobalVariables.autoMode ? "Ativado" : "Desativado"));
					break;
					
				case 0:
					System.out.println("Encerrando sistema operacional...");
					GlobalVariables.shutdown = true;

					for (Thread t : GlobalVariables.workerThreads) {
						t.interrupt();
					}

					for (Thread t : GlobalVariables.workerThreads) {
						try {
							t.join();
						} catch (InterruptedException e) {
							/* ignore */ }
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
}
