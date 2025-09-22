package com.otk.jesb.operation;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.ValidationError;

public interface OperationStructureBuilder<T> {

	T build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception;

	void validate(boolean recursively, Plan plan, Step step) throws ValidationError;

}
