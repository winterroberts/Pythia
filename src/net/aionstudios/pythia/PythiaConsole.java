package net.aionstudios.pythia;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * Manages {@link Command} input and resolution, including
 * help information and memory where available.
 * @author Winter Roberts
 */
public class PythiaConsole {
	
	private static PythiaConsole self;
	private static Thread consoleThread;
	private static boolean running;
	
	private Set<Command> commands;
	
	/**
	 * A Singleton constructor that initializes the console input
	 * and threadding for {@link Command} input.
	 */
	private PythiaConsole() {
		self = this;
		commands = new TreeSet<>();
		running = false;
		consoleThread = new Thread(new Runnable() {

			@Override
			public void run() {
				boolean useConsole = true;
				Console c = System.console();
				Scanner s = null;
				if(c==null){
					useConsole = false;
					s = new Scanner(System.in);
				}
				while(running) {
					String cPre = "";
					String[] command;
					if(useConsole) cPre = c.readLine("> ");
					else cPre = s.nextLine();
					if(cPre.matches("^[ \\t\\r\\n\\s]*$")) continue;
					command = cPre.split("\\s+");
					boolean found = false;
					System.out.println("Issued command: "+cPre);
					if(command[0].equals("?")||command[0].equals("help")) {
						String helpString = "Commands and usage:\r\n";
						for(Command co : commands) {
							helpString = helpString.concat("'"+co.getCommand()+"' "+co.getHelp()+"\r\n");
						}
						System.out.println(helpString);
					} else {
						for(Command co : commands) {
							if(co.getCommand().equals(command[0])) {
								co.execute(Arrays.copyOfRange(command, 1, command.length));
								found=true;
							}
							if(found) break;
						}
						if(!found) {
							System.out.println("Couldn't find command '"+command[0]+"'. Try '?' for help.");
						}
					}
				}
				if(s!=null) {
					s.close();
				}
			}
			
		});
		consoleThread.setName("Pythia-Console");
	}
	
	/**
	 * @return A singleton instance of {@link JDCConsole}, constructing it if
	 * this is the first call to the method.
	 */
	public static PythiaConsole getInstance() {
		return self != null ? self : new PythiaConsole();
	}
	
	/**
	 * Starts, or restarts, the application's command processing thread.
	 */
	public void startConsoleThread() {
		if(!consoleThread.isAlive()) {
			running = true;
			consoleThread.start();
		}
	}
	
	/**
	 * Stops the application's command processing thread.
	 */
	public void stopConsoleThread() {
		running = false;
	}
	
	/**
	 * @return True if the consoleThread has been initialized and is alive, false otherwise.
	 */
	public boolean isAlive() {
		return consoleThread!=null&&consoleThread.isAlive();
	}
	
	/**
	 * @param c Registers a {@link Command}, failing for duplicate named entries.
	 */
	public void registerCommand(Command c) {
		for(Command s : commands) {
			if(c==s||c.getCommand().equals(s.getCommand())){
				System.out.println("Duplicate command failed! '"+c.getCommand()+"'");
				return;
			}
		}
		commands.add(c);
	}

}
