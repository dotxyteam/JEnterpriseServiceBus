package com.otk.jesb.operation;

import xy.reflect.ui.info.ResourcePath;

/**
 * Allows to classify and describe a specific type of {@link Operation}.
 * 
 * @author olitank
 *
 */
public interface OperationMetadata<T extends Operation> {

	/**
	 * @return The display name of the specific {@link Operation} type.
	 */
	String getOperationTypeName();

	/**
	 * @return The specific {@link OperationBuilder} type.
	 */
	Class<? extends OperationBuilder<T>> getOperationBuilderClass();

	/**
	 * @return The path to the icon describing the specific {@link Operation} type.
	 */
	ResourcePath getOperationIconImagePath();

	/**
	 * @return The category name of the specific {@link Operation} type.
	 */
	String getCategoryName();

}
