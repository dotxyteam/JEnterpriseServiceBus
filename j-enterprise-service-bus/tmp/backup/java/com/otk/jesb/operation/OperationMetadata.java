package com.otk.jesb.operation;

import xy.reflect.ui.info.ResourcePath;

public interface OperationMetadata {
	
	String getOperationTypeName();
	Class<? extends OperationBuilder> getOperationBuilderClass();
	ResourcePath getOperationIconImagePath();
	String getCategoryName();

}
