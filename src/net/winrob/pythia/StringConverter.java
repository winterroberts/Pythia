package net.winrob.pythia;

import java.util.Iterator;

public class StringConverter implements ArgumentConverter<String> {

	@Override
	public String parseArg(Iterator<String> itr) {
		return itr.next();
	}

}
