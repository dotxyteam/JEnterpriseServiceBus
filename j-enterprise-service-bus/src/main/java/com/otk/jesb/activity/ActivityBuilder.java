package com.otk.jesb.activity;

import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.instantiation.CompilationContext;
import com.otk.jesb.instantiation.Function;

public interface ActivityBuilder {

	Activity build(ExecutionContext context) throws Exception;

	Class<?> getActivityResultClass();

	CompilationContext findFunctionCompilationContext(Function function, ValidationContext validationContext);

}
