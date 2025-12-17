package com.otk.jesb.resource;

import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.Solution;

/**
 * This is the base for shared configurations of {@link Solution} instances.
 * 
 * @author olitank
 *
 */
public abstract class Resource extends Asset implements ResourceStructure {

	/**
	 * The naming constructor.
	 * 
	 * @param name The new instance name
	 */
	public Resource(String name) {
		super(name);
	}

	/**
	 * The default constructor.
	 */
	public Resource() {
		super();
	}

}
