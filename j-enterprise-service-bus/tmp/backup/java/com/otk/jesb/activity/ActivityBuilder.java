package com.otk.jesb.operation;

import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.InstantiationFunction;

public interface OperationBuilder {

	Operation build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception;

	Class<?> getOperationResultClass();

	CompilationContext findFunctionCompilationContext(InstantiationFunction function, Step currentStep, Plan currentPlan);

}
