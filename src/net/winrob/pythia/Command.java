package net.winrob.pythia;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public abstract class Command {
	
	public CommandLine commandLine;
	
	public abstract void execute();
	
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Option {
		
		String name();
		
		String alias();
		
		String[] default_() default {};
		
		String description() default "";
		
		boolean useNamed() default false;
		
	}
	
	@Retention(RUNTIME)
	@Target(FIELD)
	public @interface Parameter {
		
		String name();
		
		int arity();
		
		String description() default "";
		
	}
	
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface SubCommand {
		
		String keyword();
		
		Class<? extends Command> command();
		
	}
	
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface Description {
		
		String description();
		
	}

}
