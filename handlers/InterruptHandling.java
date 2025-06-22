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

					pm.dealloc(GlobalVariables.running.getId());
					
					GlobalVariables.ready.remove(GlobalVariables.running);
					GlobalVariables.running.setStates(ProcessStates.finished);

					GlobalVariables.semaphoreScheduler.release();
					break;
				case ioFinished:
					System.out.println("IO Finished");

					PCB p = GlobalVariables.blockedIO.poll();
					GlobalVariables.ready.add(p);
					p.setStates(ProcessStates.ready);

					if (GlobalVariables.running.getId() == -1 || GlobalVariables.ready.isEmpty()) GlobalVariables.semaphoreScheduler.release();
					break;
				case intEnderecoInvalido:
					System.out.println("Endereco invalido - A execucao do processo " + GlobalVariables.running.getId() + " sera pausada e o processo cancelado.");

					pm.dealloc(GlobalVariables.running.getId());
					GlobalVariables.running.setStates(ProcessStates.finished);

					GlobalVariables.semaphoreScheduler.release();
					break;
				case pageFault:
					// Remove da fila de prontos e adiciona na fila de bloqueados
					GlobalVariables.ready.remove(GlobalVariables.running);
					GlobalVariables.blockedVM.add(GlobalVariables.running);

					// Atualiza o estado do processo para bloqueado e salvo o contexto
					GlobalVariables.running.setStates(ProcessStates.blocked);
					GlobalVariables.running.setContext(hw.cpu.pc, hw.cpu.reg);

					// Seta o ID da requisição de página
					GlobalVariables.vmRequest.setId(GlobalVariables.running.getId());

					// Seta o endereço lógico da página que causou a falha
					GlobalVariables.semaphoreScheduler.release();

					// Libera o semáforo da VM para aguardar a requisição de página
					GlobalVariables.semaphoreVm.release();
					break;
				case pageSaved, pageLoaded:
					PCB process = GlobalVariables.blockedVM.poll();
					GlobalVariables.ready.add(process);
					process.setStates(ProcessStates.ready);

					if (GlobalVariables.running.getId() == -1 || GlobalVariables.ready.isEmpty()) GlobalVariables.semaphoreScheduler.release();
					break;
				default:
					System.out.println("Interrupção não tratada: " + irpt);
					break;
			}
		}
	}
