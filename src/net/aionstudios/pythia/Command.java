package net.aionstudios.pythia;

/**
 * Defines the functionality for a console command in JDC Server.
 * @author Winter Roberts
 */
public abstract class Command implements Comparable<Command> {
	
	private String command;
	
	/**
	 * Constructs a new command with the given name, failing if one with that name is
	 * already registered to the {@link JDCConsole}.
	 * @param command The String name of this command.
	 */
	public Command(String command) {
		this.command = command;
		PythiaConsole.getInstance().registerCommand(this);
	}
	
	/**
	 * @return The string used to call this command.
	 */
	public String getCommand() {
		return command;
	}
	
	/**
	 * Defines the execution behavior of this command.
	 * @param args Arguments passed after the command.
	 */
	public abstract void execute(String... args);
	
	/**
	 * @return Prints the help information, i.e. USAGE of this command.
	 */
	public abstract String getHelp();
	
	@Override
	public int compareTo(Command other) {
		return command.compareTo(other.getCommand());
	}
	
}
