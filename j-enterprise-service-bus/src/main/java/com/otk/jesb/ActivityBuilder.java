package com.otk.jesb;

import com.otk.jesb.Plan.ExecutionContext;

public abstract class ActivityBuilder {

	public abstract Activity build(ExecutionContext context) throws Exception;

	public abstract Class<? extends ActivityResult> getResultClass();

	public PathExplorer getResultPathExplorer() {
		Class<? extends ActivityResult> resultClass = getResultClass();
		if (resultClass == null) {
			return null;
		}
		return new PathExplorer(resultClass.getName());
	}

}
