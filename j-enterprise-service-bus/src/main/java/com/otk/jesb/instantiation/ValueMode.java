package com.otk.jesb.instantiation;

/**
 * Allows to classify value specification object.
 * 
 * @author olitank
 *
 */
public enum ValueMode {
	/**
	 * Static value specification (typically {@link InstanceBuilder} or primitive
	 * constant) category.
	 */
	PLAIN,

	/**
	 * Dynamic value specification (typically {@link InstantiationFunction})
	 * category.
	 */
	FUNCTION
}