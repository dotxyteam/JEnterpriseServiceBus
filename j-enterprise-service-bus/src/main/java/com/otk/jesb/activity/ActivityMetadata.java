package com.otk.jesb.activity;

import xy.reflect.ui.info.ResourcePath;

public interface ActivityMetadata {
	
	String getActivityTypeName();
	Class<? extends ActivityBuilder> getActivityBuilderClass();
	ResourcePath getActivityIconImagePath();

}
