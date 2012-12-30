package com.engine.interpretation;

import java.util.Comparator;

/**
 * A comparator class to sort strings by length, longest to shortest.
 */
public class StringCompare implements Comparator<String> {
	
    /**
     * Compares 2 strings by length
     * @param o1
     * @param o2
     * @return Comparison integer
     */
    @Override
    public int compare (String o1, String o2) {

		if (o1.length() < o2.length()) {
			return 1;
		}
		else if (o1.length() > o2.length()) {
			return -1;
		}
		return 0;
	}
}
