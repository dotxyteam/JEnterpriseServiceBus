package com.otk.jesb.activation;

import com.otk.jesb.ValidationError;
import com.otk.jesb.solution.Plan;

/**
 * This type was extracted from {@link Activator} to allow the convenient
 * distribution (split) of its sub-types information into structured member
 * types.
 * 
 * @author olitank
 *
 */
public interface ActivatorStructure {

	/**
	 * Validates the current object data.
	 * 
	 * @param recursively Whether the validation is recursively executed on
	 *                    sub-objects or not.
	 * 
	 * @param plan        The associated {@link Plan} instance.
	 * 
	 * @throws ValidationError If the current object is considered as invalid.
	 */
	public abstract void validate(boolean recursively, Plan plan) throws ValidationError;

}
