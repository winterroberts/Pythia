package net.winrob.pythia;

import java.util.Iterator;

/**
 * Converts {@link String} argument(s) to a typed object <T>.
 * 
 * @author Winter Roberts
 *
 * @param <T> The type an argument should be converted to.
 */
public interface ArgumentConverter<T> {
	
	/**
	 * Parse argument(s), converting them to a typed object.
	 * 
	 * @param itr String iterator to be consumed.
	 * @return The parsed type.
	 */
	public T parseArg(Iterator<String> itr);

}
