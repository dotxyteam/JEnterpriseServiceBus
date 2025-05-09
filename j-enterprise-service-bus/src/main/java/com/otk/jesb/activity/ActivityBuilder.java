package com.otk.jesb.activity;

import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.ValidationError;
import com.otk.jesb.instantiation.InstantiationFunction;

public interface ActivityBuilder {

	Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception;

	Class<?> getActivityResultClass(Plan currentPlan, Step currentStep);

	InstantiationFunctionCompilationContext findFunctionCompilationContext(InstantiationFunction function, Step currentStep, Plan currentPlan);

	void validate(Plan plan, Step step) throws ValidationError;

}
