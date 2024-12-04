package com.otk.jesb;

import com.otk.jesb.Plan.ExecutionContext;

public interface ActivityBuilder {

	public Activity build(ExecutionContext context) throws Exception;

}
