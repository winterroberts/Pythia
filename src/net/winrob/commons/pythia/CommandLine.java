package net.winrob.commons.pythia;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.winrob.commons.pythia.Command.Description;
import net.winrob.commons.pythia.Command.Option;
import net.winrob.commons.pythia.Command.Parameter;
import net.winrob.commons.pythia.Command.SubCommand;
import net.winrob.hestia.data.ComparablePair;
import net.winrob.hestia.data.Pair;

/**
 * A collection of commands which accepts and interprets included {@link Command}s by their keywords and definition.
 * 
 * @author Winter Roberts
 */
public class CommandLine {
	
	private Map<String, CommandInterpreter> commands;
	
	private Map<Class<?>, ArgumentConverter<?>> converters;
	
	/**
	 * Creates a new command line, with default support for {@link StringConverter} and {@link IntegerConverter} arguments.
	 */
	public CommandLine() {
		commands = new HashMap<>();
		converters = new HashMap<>();
		addConverter(String.class, new StringConverter());
		addConverter(Integer.class, new IntegerConverter());
	}
	
	/**
	 * Adds a {@link CommandInterpreter} invoked by the keyword.
	 * 
	 * @param keyword The keyword which should invoke the interpreter.
	 * @param command The {@link Command} class for which the {@link CommandInterpreter} should be created.
	 * @throws KeyWordCollisionException If the keyword is already in use.
	 */
	public void addCommand(String keyword, Class<? extends Command> command) {
		if (commands.containsKey(keyword)) throw new KeyWordCollisionException("Keyword '" + keyword + "' is already in use!");
		commands.put(keyword, new CommandInterpreter(keyword, command));
	}

	/**
	 * Interprets command line arguments passed by the parent {@link CommandLine}, according to the {@link Command} class definition.
	 */
	public class CommandInterpreter {
		
		private String keyword;
		private Class<? extends Command> command;
		
		CommandLine subCommandLine;
		private Map<String, Option> optionAlias;
		private Map<Option, Field> optionFields;
		private Map<Integer, Pair<Parameter, Field>> parameters;
		
		private Description description;
		
		/**
		 * Uses the {@link Command} class definition to construct an interpreter.
		 * 
		 * @param keyword The keyword which invokes the interpreter.
		 * @param command The {@link Command} class for which this interpreter should be created.
		 */
		protected CommandInterpreter(String keyword, Class<? extends Command> command) {
			this.keyword = keyword;
			this.command = command;
			optionAlias = new HashMap<>();
			optionFields = new HashMap<>();
			parameters = new TreeMap<>();

			SubCommand[] subCommands = command.getAnnotationsByType(SubCommand.class);
			for (SubCommand sc : subCommands) {
				if (subCommandLine == null) subCommandLine = new CommandLine();
				subCommandLine.addCommand(sc.keyword(), sc.command());
			}
			
			for (Field f : command.getDeclaredFields()) {
				f.setAccessible(true);
				if (f.isAnnotationPresent(Parameter.class)) {
					Parameter p = f.getAnnotation(Parameter.class);
					if (parameters.containsKey(p.arity())) {
						Pair<Parameter, Field> other = parameters.get(p.arity());
						String arityErr = String.format(
								"Arity (%d) overloaded, usage by fields %s (%s) and %s (%s)",
								p.arity(), f.getName(), f.getType().getTypeName(), other.getSecond().getName(), other.getSecond().getType().getTypeName());
						throw new RuntimeException(arityErr);
					}
					parameters.put(p.arity(), new Pair<>(p, f));
				} else if (f.isAnnotationPresent(Option.class)) {
					Option o = f.getAnnotation(Option.class);
					optionFields.put(o, f);
					if (o.useNamed()) addOptionAlias("-" + o.name(), o);
					addOptionAlias(o.alias(), o);
				}
			}
			
			Description[] d = command.getAnnotationsByType(Description.class);
			if (d != null && d.length != 0) {
				description = d[d.length - 1];
			}
		}
		
		/**
		 * @param alias The alias that should be created.
		 * @param option The {@link Command} {@link Option} that should be aliased.
		 */
		public void addOptionAlias(String alias, Option option) {
			if (optionAlias.containsKey(alias)) throw new KeyWordCollisionException("Option keyword '" + alias + "' is already in use!");
			optionAlias.put(alias, option);
		}
		
		/**
		 * Interprets {@link CommandLine} input, returning a populated and instantiated {@link Command} instance.
		 * 
		 * @param argItr An iterator over the arguments to the interpreter.
		 * @return A {@link Command} which has been populated with the interpreted arguments, which may be a sub command.
		 * @throws CommandInterpretException If the arguments were malformed or insufficient to populate the identified {@link Command} instance.
		 * @throws IllegalAccessException If any {@link Field} in the {@link Command} was inaccessible to be populated by an interpreted argument.
		 */
		protected Command interpret(Iterator<String> argItr) throws CommandInterpretException, IllegalAccessException {
			String nextWord = argItr.hasNext() ? argItr.next() : null;
			if (subCommandLine != null && nextWord != null && subCommandLine.hasCommandWord(nextWord)) {
				return subCommandLine.commands.get(nextWord).interpret(argItr);
			} else {
				Command c = ObjectInstantiator.getInstance().newInstance(command);
				try {
					c.getClass().getField("commandLine").set(c, CommandLine.this);
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
						| SecurityException e) {
					e.printStackTrace();
				}
				Set<Option> optionAssigned = new HashSet<>();
				
				String word = nextWord;
				while (word != null) {
					if (!word.startsWith("-")) {
						break;
					}
					word = word.substring(1);
					// TODO missing optionAlias, already assigned
					Option o = optionAlias.get(word);
					Field f = optionFields.get(o);
					if (f.getType().isAssignableFrom(Boolean.class)) f.set(c, true);
					else f.set(c, convertArgument(f.getType(), argItr));
					optionAssigned.add(o);
					word = argItr.hasNext() ? argItr.next() : null;
				}
				List<String> params = new LinkedList<>();
				params.add(word);
				while (argItr.hasNext()) {
					params.add(argItr.next());
				}
				Iterator<String> paramItr = params.iterator();
				for (Entry<Integer, Pair<Parameter, Field>> pe : parameters.entrySet()) {
					if (!paramItr.hasNext()) throw new CommandInterpretException("Incomplete parameter list!");
					Pair<Parameter, Field> pPair = pe.getValue();
					Field f = pPair.getSecond();
					f.set(c, convertArgument(f.getType(), paramItr));
				}
				for (Entry<Option, Field> oe : optionFields.entrySet()) {
					Option o = oe.getKey();
					Field f = oe.getValue();
					if (!optionAssigned.contains(o)) {
						if (o.default_().length == 0) throw new CommandInterpretException("Missing required option '" + o.name() + "'");
						else f.set(c, convertArgument(f.getType(), Arrays.stream(o.default_()).iterator()));
					}
				}
				return c;
			}
		}
		
		/**
		 * @return True if this interpreter has a sub-{@link CommandLine} (the {@link Command} defined a {@link SubCommand}, false otherwise.
		 */
		public boolean hasSubCommandLine() {
			return subCommandLine != null;
		}
		
		/**
		 * @return The sub-{@link CommandLine} if it exists, null otherwise.
		 */
		public CommandLine getSubCommandLine() {
			return hasSubCommandLine() ? subCommandLine : null;
		}
		
		/**
		 * Converts argument(s) from input to a typed object.
		 * 
		 * @param <T> The type of the object for which argument(s) should be interpreted.
		 * @param clazz The expected class of the output typed object.
		 * @param argument An iterator over the arguments to the interpreter.
		 * @return A typed object, the result of converting argument(s).
		 */
		@SuppressWarnings("unchecked")
		private <T> T convertArgument(Class<T> clazz, Iterator<String> argument) {
			return (T) converters.get(clazz).parseArg(argument);
		}
		
		private String helpDialog;
		
		/**
		 * Builds the help dialog for the {@link Command} this object interprets, printing it to stdout.
		 */
		public void showHelpDialog() {
			if (helpDialog == null) {
				StringBuilder builder = new StringBuilder();
				List<ComparablePair<String, Option>> options = new LinkedList<>();
				for (Option o : optionFields.keySet()) {
					options.add(ComparablePair.of(o.alias(), o));
				}
				Collections.sort(options);
				
				builder.append(keyword);
				if (!options.isEmpty()) {
					builder.append(" [-");
					for (Pair<String, Option> pair : options) {
						builder.append(pair.getFirst());
					}
					builder.append("]");
				}
				
				if (!parameters.isEmpty()) {
					for (Entry<Integer, Pair<Parameter, Field>> p : parameters.entrySet()) {
						Parameter param = p.getValue().getFirst();
						builder.append(" <" + param.name() + ">");
					}
				}
				if (description != null) {
					builder.append("\r\n");
					builder.append("  ");
					builder.append(description.description());
					builder.append("\r\n");
				}
				
				builder.append("\r\n");
				
				if (!options.isEmpty()) {
					builder.append("  OPTIONS:\r\n");
					for (Pair<String, Option> pair : options) {
						builder.append("    -");
						builder.append(String.format("%-9s", pair.getFirst()));
						builder.append(wrapText(pair.getSecond().description(), "              ", 58));
						builder.append("\r\n");
					}
				}
				
				if (!parameters.isEmpty()) {
					builder.append("  PARAMETERS:\r\n");
					for (Entry<Integer, Pair<Parameter, Field>> p : parameters.entrySet()) {
						Parameter param = p.getValue().getFirst();
						builder.append("    ");
						builder.append(String.format("%-10s", param.name()));
						builder.append(wrapText(param.description(), "              ", 58));
						builder.append("\r\n");
					}
				}
				helpDialog = builder.toString();
			}
			System.out.println(helpDialog);
		}
		
		/**
		 * Adds space and newline characters to the input text to word-wrap the input string.
		 * If a word exceeds the line's wrap length, it is wrapped inelegantly.
		 * 
		 * @param text The text that should be word-wrapped.
		 * @param wrapStart The number of blank spaces which should occur before text.
		 * @param wrapLength The maximum length of a line before word-wrapping should occur.
		 * @return The word-wrapped version of the input text.
		 */
		private String wrapText(String text, String wrapStart, int wrapLength) {
			StringBuilder sb = new StringBuilder(text);

			int i = 0;
			while (i + wrapLength < sb.length() && (i = sb.lastIndexOf(" ", i + wrapLength)) != -1) {
			    sb.replace(i, i + 1, "\r\n" + wrapStart);
			}

			return sb.toString();
		}
		
	}
	
	/**
	 * Executes an input, which should be interpreted from spaces.
	 * 
	 * @param args The arguments which should execute a {@link Command}.
	 * @throws CommandInterpretException If no {@link CommandInterpeter} could be found matching the input, including if the input is malformed.
	 * @throws IllegalAccessException If any {@link Field} in the {@link Command} was inaccessible to be populated by an interpreted argument.
	 */
	public void execute(String... args) throws CommandInterpretException, IllegalAccessException {
		execute(Arrays.stream(args).iterator());
	}
	
	/**
	 * Executes an input, which should be interpreted from spaces.
	 * 
	 * @param args The arguments which should execute a {@link Command}.
	 * @throws CommandInterpretException If no {@link CommandInterpeter} could be found matching the input, including if the input is malformed.
	 * @throws IllegalAccessException If any {@link Field} in the {@link Command} was inaccessible to be populated by an interpreted argument.
	 */
	public void execute(List<String> args) throws CommandInterpretException, IllegalAccessException {
		execute(args.iterator());
	}
	
	/**
	 * Executes an input, which should be interpreted from spaces.
	 * 
	 * @param argItr An iterator over the arguments which should execute a {@link Command}.
	 * @throws CommandInterpretException If no {@link CommandInterpeter} could be found matching the input, including if the input is malformed.
	 * @throws IllegalAccessException If any {@link Field} in the {@link Command} was inaccessible to be populated by an interpreted argument.
	 */
	private void execute(Iterator<String> argItr) throws CommandInterpretException, IllegalAccessException {
		if (!argItr.hasNext()) throw new CommandInterpretException("Out of tokens!");
		String commandWord = argItr.next();
		CommandInterpreter interpreter = commands.get(commandWord);
		if (interpreter == null) throw new CommandInterpretException("Command keyword '" + commandWord + "' not found!");
		interpreter.interpret(argItr).execute();
	}
	
	/**
	 * Adds an typed object {@link ArgumentConverter} to this command line.
	 * 
	 * @param <T> The type of the object for which argument(s) should be interpreted.
	 * @param clazz The expected class of the output typed object.
	 * @param converter The {@link ArgumentConverter} which creates the typed object.
	 */
	public <T> void addConverter(Class<T> clazz, ArgumentConverter<T> converter) {
		converters.put(clazz, converter);
	}
	
	/**
	 * Gets the {@link CommandInterpreter} for a keyword.
	 * 
	 * @param word The keyword for which a {@link CommandInterpreter} should be found.
	 * @return The associated {@link CommandInterpreter} if it exists, otherwise null.
	 */
	public CommandInterpreter getInterpreterForCommandWord(String word) {
		return hasCommandWord(word) ? commands.get(word) : null;
	}
	
	/**
	 * @param word A keyword which may be associated with an {@link CommandInterpreter}.
	 * @return True if a {@link CommandInterpreter} is associated with the keyword, false otherwise.
	 */
	public boolean hasCommandWord(String word) {
		return commands.containsKey(word);
	}
	
	/**
	 * Thrown when an {@link CommandInterpreter} is unable to successfully interpret command line input.
	 */
	public class CommandInterpretException extends Exception {
		
		private static final long serialVersionUID = -8133225772318315478L;

		public CommandInterpretException(String message) {
			super(message);
		}
		
	}
	
	/**
	 * Thrown when a keyword collision occurs because a {@link Command} is already registered to it.
	 */
	private class KeyWordCollisionException extends RuntimeException {

		private static final long serialVersionUID = -7355538200994919749L;
		
		public KeyWordCollisionException(String message) {
			super(message);
		}
		
	}
	
}
