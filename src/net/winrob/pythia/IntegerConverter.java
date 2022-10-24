package net.winrob.pythia;

import java.util.Iterator;

public class IntegerConverter implements ArgumentConverter<Integer> {

	@Override
	public Integer parseArg(Iterator<String> itr) {
		return Integer.parseInt(itr.next());
	}

}
