package com.otk.jesb.operation;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;

/**
 * This is the base of the specifications from which instances of
 * {@link Operation} can be created dynamically.
 * 
 * @author olitank
 *
 * @param <T> The specific {@link Operation} type.
 */
public interface OperationBuilder<T extends Operation> extends OperationStructureBuilder<T> {

	/**
	 * @param currentPlan The current plan.
	 * @param currentStep The current step.
	 * @return The class of objects returned by the related
	 *         {@link Operation#execute()}.
	 */
	Class<?> getOperationResultClass(Plan currentPlan, Step currentStep);

}
