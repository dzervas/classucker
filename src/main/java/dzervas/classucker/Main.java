package dzervas.classucker;

import com.sun.tools.attach.VirtualMachine;

public class Main {
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: java -jar classucker.jar <PID> <path-to-agent-jar>");
			return;
		}

		String pid = args[0];
		String agentPath = args[1];

		try {
			VirtualMachine vm = VirtualMachine.attach(pid);
			vm.loadAgent(agentPath);
			vm.detach();
			System.out.println("Agent loaded successfully.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
