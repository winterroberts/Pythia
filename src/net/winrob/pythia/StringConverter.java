package net.winrob.pythia;

import java.util.Iterator;

/**
 * An {@link ArgumentConverter} for a single argument to a {@link String}.
 * 
 * @author Winter Roberts
 */
public class StringConverter implements ArgumentConverter<String> {

	@Override
	public String parseArg(Iterator<String> itr) {
		return itr.next();
	}

}
