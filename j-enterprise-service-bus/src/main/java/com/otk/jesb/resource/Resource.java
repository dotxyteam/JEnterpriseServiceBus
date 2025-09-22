package com.otk.jesb.resource;

import com.otk.jesb.solution.Asset;

public abstract class Resource extends Asset implements ResourceStructure {

	public Resource() {
	}

	public Resource(String name) {
		super(name);
	}

}
