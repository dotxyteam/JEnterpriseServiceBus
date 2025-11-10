package com.otk.jesb.operation;

import com.otk.jesb.solution.Plan;

/**
 * This is the base of all atomic activities in a {@link Plan}.
 * 
 * @author olitank
 *
 */
public interface Operation {

	/**
	 * Performs this {@link Operation} actions.
	 * 
	 * @return The result of the {@link Operation} execution.
	 * @throws Throwable If this execution fails.
	 */
	Object execute() throws Throwable;

}