package com.otk.jesb.activation;

import xy.reflect.ui.info.ResourcePath;

public interface ActivationStrategyMetadata {
	
	ResourcePath getActivationStrategyIconImagePath();
	Class<? extends ActivationStrategy> getActivationStrategyClass();
	String getActivationStrategyName();
	
}
