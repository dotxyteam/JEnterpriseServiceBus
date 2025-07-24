package com.otk.jesb.operation;

import xy.reflect.ui.info.ResourcePath;

public interface OperationMetadata<T extends Operation> {

	String getOperationTypeName();

	Class<? extends OperationBuilder<T>> getOperationBuilderClass();

	ResourcePath getOperationIconImagePath();

	String getCategoryName();

}
