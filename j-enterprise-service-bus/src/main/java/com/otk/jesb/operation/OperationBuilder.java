package com.otk.jesb.operation;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;

public interface OperationBuilder<T extends Operation> extends OperationStructureBuilder<T> {

	Class<?> getOperationResultClass(Plan currentPlan, Step currentStep);

}
