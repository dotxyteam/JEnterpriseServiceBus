package com.otk.jesb.resource;

import com.otk.jesb.ValidationError;

public interface ResourceStructure {

	public void validate(boolean recursively) throws ValidationError;

}
