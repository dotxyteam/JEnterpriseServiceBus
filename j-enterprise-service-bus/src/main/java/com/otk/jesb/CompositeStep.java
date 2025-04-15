package com.otk.jesb;

import java.util.List;

import com.otk.jesb.Plan.ValidationContext.VariableDeclaration;
import com.otk.jesb.activity.ActivityMetadata;

public abstract class CompositeStep extends Step {
	
	protected abstract List<VariableDeclaration> getChildrenVariableDeclarations();

	public CompositeStep(ActivityMetadata activityMetadata) {
		super(activityMetadata);
	}

}
