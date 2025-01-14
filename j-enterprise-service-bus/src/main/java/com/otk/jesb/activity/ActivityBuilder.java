package com.otk.jesb.activity;

import com.otk.jesb.InstanceBuilder.DynamicValue;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;

public interface ActivityBuilder {

	Activity build(ExecutionContext context) throws Exception;

	Class<?> getActivityResultClass();

	boolean completeValidationContext(ValidationContext validationContext, DynamicValue currentDynamicValue);

}
