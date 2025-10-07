package com.otk.jesb.resource;

import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.Solution;

/**
 * This is the base for shared configurations of a {@link Solution}.
 * 
 * @author olitank
 *
 */
public abstract class Resource extends Asset implements ResourceStructure {

	public Resource() {
	}

	public Resource(String name) {
		super(name);
	}

}
