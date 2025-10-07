package com.otk.jesb.operation;

import xy.reflect.ui.info.ResourcePath;

/**
 * Allows to classify and describe a specific type of {@link Operation}.
 * 
 * @author olitank
 *
 */
public interface OperationMetadata<T extends Operation> {

	String getOperationTypeName();

	Class<? extends OperationBuilder<T>> getOperationBuilderClass();

	ResourcePath getOperationIconImagePath();

	String getCategoryName();

}
