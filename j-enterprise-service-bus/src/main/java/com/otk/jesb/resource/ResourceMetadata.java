package com.otk.jesb.resource;

import xy.reflect.ui.info.ResourcePath;

/**
 * Allows to classify and describe a specific type of {@link Resource}.
 * 
 * @author olitank
 *
 */

public interface ResourceMetadata {

	ResourcePath getResourceIconImagePath();

	Class<? extends Resource> getResourceClass();

	String getResourceTypeName();

}
