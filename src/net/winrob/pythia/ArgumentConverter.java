package net.winrob.pythia;

import java.util.Iterator;

public interface ArgumentConverter<T> {
	
	public T parseArg(Iterator<String> itr);

}
