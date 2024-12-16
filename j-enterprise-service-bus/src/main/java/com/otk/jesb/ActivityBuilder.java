package com.otk.jesb;

import com.otk.jesb.Plan.ExecutionContext;

public interface ActivityBuilder {

	 Activity build(ExecutionContext context) throws Exception;

	 Class<? extends ActivityResult> getActivityResultClass();

}
