package com.otk.jesb.resource;

import com.otk.jesb.ValidationError;

/**
 * This type was extracted from {@link Resource} to allow the convenient
 * distribution (split) of its sub-types information into structured member
 * types.
 * 
 * @author olitank
 *
 */
public interface ResourceStructure {

	/**
	 * Validates the current object data.
	 * 
	 * @param recursively Whether the validation is recursively executed on
	 *                    sub-objects or not.
	 * @throws ValidationError If the current object is considered as invalid.
	 */
	public void validate(boolean recursively) throws ValidationError;
}
