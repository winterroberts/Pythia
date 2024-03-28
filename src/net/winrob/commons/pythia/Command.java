package net.winrob.commons.pythia;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines the {@link Option}s, {@link Parameter}s, {@link SubCommand}s, and {@link Description} for a command along with its execution.
 * 
 * @author Winter Roberts
 */
public abstract class Command {
	
	public CommandLine commandLine;
	
	public abstract void execute();
	
	/**
	 * Defines a named option and its alias, along with a possible description and default value.
	 */
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Option {
		
		String name();
		
		String alias();
		
		String[] default_() default {};
		
		String description() default "";
		
		boolean useNamed() default false;
		
	}
	
	/**
	 * Defines a named parameter and assigns its arity (position) in the command.
	 */
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Parameter {
		
		String name();
		
		int arity();
		
		String description() default "";
		
	}
	
	/**
	 * Defines a subcommand, accessed by its keyword usage immediately following the parent command's keyword.
	 */
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface SubCommand {
		
		String keyword();
		
		Class<? extends Command> command();
		
	}
	
	/**
	 * Provides a description of this command used in help dialog.
	 */
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface Description {
		
		String description();
		
	}

}
