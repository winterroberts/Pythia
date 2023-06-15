package net.winrob.pythia;

import java.io.Console;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * An {@link CommandLine} that accepts inputs from the application stdin.
 */
public class ConsoleCommandLine extends CommandLine {
	
	private static ConsoleCommandLine self;
	private Thread consoleThread;
	private boolean running = false;
	
	private ConsoleCommandLine() {
		self = this;
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
					try {
						if(useConsole) cPre = c.readLine("> ");
						else cPre = s.nextLine();
					} catch (NoSuchElementException e) {
						continue;
					}
					if(cPre.matches("^[ \\t\\r\\n\\s]*$")) continue;
					System.out.println("Issued command: "+cPre);
					
					String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
					String[] tokens = cPre.split(regex);
					for (int i = 0; i < tokens.length; i++) {
						String token = tokens[i];
						if (token.startsWith("\"") && token.endsWith("\"")) token = token.substring(1, token.length() - 1);
						tokens[i] = token;
					}
					
					try {
						ConsoleCommandLine.getInstance().execute(tokens);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (CommandInterpretException e) {
						e.printStackTrace();
					}
				}
				if(s!=null) {
					s.close();
				}
			}
			
		});
		consoleThread.setName("Pythia-Console");
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			@Override
			public void run() {
				running = false;
			}
			
		}));
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
	 * @return True if the console thread has been initialized and is alive, false otherwise.
	 */
	public boolean isAlive() {
		return consoleThread!=null&&consoleThread.isAlive();
	}
	
	/**
	 * @return The singleton instance of the console command line.
	 */
	public static ConsoleCommandLine getInstance() {
		return self == null ? new ConsoleCommandLine() : self;
	}

}
