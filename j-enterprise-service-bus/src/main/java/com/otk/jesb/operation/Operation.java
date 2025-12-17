package com.otk.jesb.operation;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;

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
	 * @param runtime The current solution runtime.
	 * @return The result of the {@link Operation} execution.
	 * @throws Throwable If this execution fails.
	 */
	Object execute(Solution solutionInstance) throws Throwable;

}