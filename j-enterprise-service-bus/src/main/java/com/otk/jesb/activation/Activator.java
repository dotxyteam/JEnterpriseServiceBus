package com.otk.jesb.activation;

import com.otk.jesb.Structure;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Variant;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;

/**
 * This is the base of any trigger of {@link Plan} execution.
 * 
 * @author olitank
 *
 */
public abstract class Activator implements ActivatorStructure {

	/**
	 * @param solutionInstance The current solution.
	 * @return The {@link Structure} of the input that the associated {@link Plan}
	 *         would have.
	 */
	public abstract Class<?> getInputClass(Solution solutionInstance);

	/**
	 * @param solutionInstance The current solution.
	 * @return The {@link Structure} of the output that the associated {@link Plan}
	 *         would have.
	 */
	public abstract Class<?> getOutputClass(Solution solutionInstance);

	/**
	 * Enables the firing of activation events produced by external sources.
	 * 
	 * @param activationHandler The activation event handler.
	 * @param solutionInstance  The current solution.
	 * @throws Exception If the initialization fails.
	 */
	public abstract void initializeAutomaticTrigger(ActivationHandler activationHandler, Solution solutionInstance)
			throws Exception;

	/**
	 * Disables the firing of activation events produced by external sources.
	 * 
	 * @param solutionInstance The current solution.
	 * 
	 * @throws Exception If the finalization fails.
	 * 
	 */
	public abstract void finalizeAutomaticTrigger(Solution solutionInstance) throws Exception;

	/**
	 * @return Whether activation events firing is enabled.
	 */
	public abstract boolean isAutomaticTriggerReady();

	/**
	 * @return Whether this {@link Activator} allows to receive activation events
	 *         from external sources.
	 */
	public abstract boolean isAutomaticallyTriggerable();

	private Variant<Boolean> enabledVariant = new Variant<Boolean>(Boolean.class, true);

	/**
	 * @return The boolean {@link Variant} allowing to enable (true) or disable
	 *         (false) the current {@link Activator}.
	 */
	public Variant<Boolean> getEnabledVariant() {
		return enabledVariant;
	}

	/**
	 * Sets the boolean {@link Variant} allowing to enable (true) or disable (false)
	 * the current {@link Activator}.
	 * 
	 * @param enabledVariant A boolean {@link Variant}.
	 */
	public void setEnabledVariant(Variant<Boolean> enabledVariant) {
		this.enabledVariant = enabledVariant;
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance, Plan plan) throws ValidationError {
		enabledVariant.validate(solutionInstance);
	}
}
