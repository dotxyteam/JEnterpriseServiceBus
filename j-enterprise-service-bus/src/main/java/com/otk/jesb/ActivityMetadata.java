package com.otk.jesb;

import xy.reflect.ui.info.ResourcePath;

public interface ActivityMetadata {
	
	String getActivityTypeName();
	Class<? extends ActivityBuilder> getActivityBuilderClass();
	ResourcePath getActivityIconImagePath();

}
