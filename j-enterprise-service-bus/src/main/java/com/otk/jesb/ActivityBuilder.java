package com.otk.jesb;

import com.otk.jesb.Plan.ExecutionContext;

public interface ActivityBuilder {

	public abstract Activity build(ExecutionContext context) throws Exception;

	public abstract Class<? extends ActivityResult> getResultClass();

}
