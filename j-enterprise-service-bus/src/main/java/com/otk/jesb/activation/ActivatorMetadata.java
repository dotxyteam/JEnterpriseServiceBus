package com.otk.jesb.activation;

import xy.reflect.ui.info.ResourcePath;

public interface ActivatorMetadata {
	
	ResourcePath getActivatorIconImagePath();
	Class<? extends Activator> getActivatorClass();
	String getActivatorName();
	
}
