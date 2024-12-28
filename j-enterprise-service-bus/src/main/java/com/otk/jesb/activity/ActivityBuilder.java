package com.otk.jesb.activity;

import com.otk.jesb.InstanceSpecification.DynamicValue;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;

public interface ActivityBuilder {

	Activity build(ExecutionContext context) throws Exception;

	Class<? extends ActivityResult> getActivityResultClass();

	boolean completeValidationContext(ValidationContext validationContext, DynamicValue currentDynamicValue);

}
