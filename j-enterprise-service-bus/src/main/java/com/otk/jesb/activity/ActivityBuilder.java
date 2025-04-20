package com.otk.jesb.activity;

import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ExecutionInspector;
import com.otk.jesb.Step;
import com.otk.jesb.Function;
import com.otk.jesb.Plan;
import com.otk.jesb.instantiation.CompilationContext;

public interface ActivityBuilder {

	Activity build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception;

	Class<?> getActivityResultClass();

	CompilationContext findFunctionCompilationContext(Function function, Step currentStep, Plan currentPlan);

}
