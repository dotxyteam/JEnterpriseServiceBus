package com.otk.jesb.operation;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;

/**
 * This is the base of the specifications from which instances of
 * {@link Operation} can be created dynamically.
 * 
 * @author olitank
 *
 * @param <T>
 */
public interface OperationBuilder<T extends Operation> extends OperationStructureBuilder<T> {

	Class<?> getOperationResultClass(Plan currentPlan, Step currentStep);

}
