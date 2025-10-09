package com.otk.jesb.resource;

import xy.reflect.ui.info.ResourcePath;

/**
 * Allows to classify and describe a specific type of {@link Resource}.
 * 
 * @author olitank
 *
 */

public interface ResourceMetadata {

	/**
	 * @return The path to the icon describing the specific {@link Resource} type.
	 */
	ResourcePath getResourceIconImagePath();

	/**
	 * @return The specific {@link Resource} type.
	 */
	Class<? extends Resource> getResourceClass();

	/**
	 * @return The display name of the specific {@link Resource} type.
	 */
	String getResourceTypeName();

}
