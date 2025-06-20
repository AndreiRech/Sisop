package handlers;

import hardware.HW;
import utils.GlobalVariables;
import enums.Interrupts;
import enums.ProcessStates;
import os.ProcessManager;
import utils.PCB;

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

			switch (irpt) {
				case roundRobin:
					System.out.println("RoundRobin");
					GlobalVariables.semaphoreScheduler.release();
					break;
				case intInstrucaoInvalida:
					System.out.println("Instrucao invalida - A execucao do processo " + GlobalVariables.running.getId()
							+ " sera pausada e o processo cancelado.");

					// Se o processo tem uma instrucao invalida ele e desalocado da memoria e
					// retirado da fila de processos
					pm.dealloc(GlobalVariables.running.getId());
					GlobalVariables.running.setStates(ProcessStates.finished);

					GlobalVariables.semaphoreScheduler.release();
					break;
				case ioFinished:
					System.out.println("IO Finished");

					PCB p = GlobalVariables.blockedIO.poll();
					GlobalVariables.ready.add(p);
					p.setStates(ProcessStates.ready);

					break;
				case intEnderecoInvalido:
					System.out.println("Endereco invalido - A execucao do processo " + GlobalVariables.running.getId() + " sera pausada e o processo cancelado.");

					pm.dealloc(GlobalVariables.running.getId());
					GlobalVariables.running.setStates(ProcessStates.finished);

					GlobalVariables.semaphoreScheduler.release();
					break;
				case pageFault:
					System.out.println("Page Fault - A pagina nao esta carregada na memoria");

					GlobalVariables.ready.remove(GlobalVariables.running);
					GlobalVariables.blockedVM.add(GlobalVariables.running);

					GlobalVariables.running.setStates(ProcessStates.blocked);
					GlobalVariables.running.setContext(hw.cpu.pc, hw.cpu.reg);

					GlobalVariables.vmRequest.setId(GlobalVariables.running.getId());

					GlobalVariables.semaphoreScheduler.release();
					
					GlobalVariables.semaphoreVm.release();
					break;
				case pageSaved, pageLoaded:
					PCB process = GlobalVariables.blockedVM.poll();
					GlobalVariables.ready.add(process);
					process.setStates(ProcessStates.ready);

					break;
				default:
					System.out.println("Interrupção não tratada: " + irpt);
					break;
			}
		}
	}
	// TODO -> console e chamado pela syscall e deve passar os registradores para
	// salvar na memoria (deve ser o fisico nao o logico)
