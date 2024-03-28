package net.winrob.commons.pythia;

import java.util.Iterator;

/**
 * An {@link ArgumentConverter} for a single argument to an {@link Integer}.
 * 
 * @author Winter Roberts
 */
public class IntegerConverter implements ArgumentConverter<Integer> {

	@Override
	public Integer parseArg(Iterator<String> itr) {
		return Integer.parseInt(itr.next());
	}

}
