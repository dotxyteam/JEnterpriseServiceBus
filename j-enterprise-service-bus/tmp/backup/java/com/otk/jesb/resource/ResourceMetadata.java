package com.otk.jesb.resource;

import xy.reflect.ui.info.ResourcePath;

public interface ResourceMetadata {
	
	ResourcePath getResourceIconImagePath();
	Class<? extends Resource> getResourceClass();
	String getResourceTypeName();
	
}
