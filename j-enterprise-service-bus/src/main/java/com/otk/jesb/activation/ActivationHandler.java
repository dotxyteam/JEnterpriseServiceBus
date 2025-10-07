package com.otk.jesb.activation;

import com.otk.jesb.solution.Plan;

/**
 * This interface essentially has the {@link #trigger(Object)} method that is
 * called on event by an {@link Activator} to start the execution of a
 * {@link Plan}.
 * 
 * @author olitank
 *
 */
public interface ActivationHandler {

	Object trigger(Object planInput);

}
