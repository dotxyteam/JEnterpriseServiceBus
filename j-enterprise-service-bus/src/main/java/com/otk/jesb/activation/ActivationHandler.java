package com.otk.jesb.activation;

import com.otk.jesb.solution.Plan;

/**
 * This interface exists essentially to offer the {@link #trigger(Object)}
 * method.
 * 
 * @author olitank
 *
 */
public interface ActivationHandler {

	/**
	 * This method is called on event by an {@link Activator} to start the execution
	 * of a {@link Plan}.
	 * 
	 * @param planInput The plan input data or null.
	 * @return The plan output data or null.
	 */
	Object trigger(Object planInput);

}
