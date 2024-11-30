package com.otk.jesb;

import com.otk.jesb.Plan.ExecutionContext;

public class ActivityBuilder extends ObjectSpecification{

	@Override
	public Activity build(ExecutionContext context) throws Exception {
		return (Activity) super.build(context);
	}
	
	public ObjectSpecificationFacade getFacade() {
		return new ObjectSpecificationFacade(null, this);
	}
}
