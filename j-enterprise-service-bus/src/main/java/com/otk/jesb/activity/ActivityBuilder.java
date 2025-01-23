package com.otk.jesb.activity;

import com.otk.jesb.InstanceBuilder.Function;
import com.otk.jesb.InstanceBuilder.VerificationContext;
import com.otk.jesb.Plan.ExecutionContext;

public interface ActivityBuilder {

	Activity build(ExecutionContext context) throws Exception;

	Class<?> getActivityResultClass();

	boolean completeVerificationContext(VerificationContext verificationContext, Function currentFunction);

}
