package com.otk.jesb;

public interface ActivityMetadata {
	
	String getActivityTypeName();
	Class<? extends ActivityBuilder> getActivityBuilderClass();

}
