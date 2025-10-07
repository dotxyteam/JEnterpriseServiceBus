package com.otk.jesb.activation;

import xy.reflect.ui.info.ResourcePath;

/**
 * Allows to classify and describe a specific type of {@link Activator}.
 * 
 * @author olitank
 *
 */
public interface ActivatorMetadata {

	ResourcePath getActivatorIconImagePath();

	Class<? extends Activator> getActivatorClass();

	String getActivatorName();

}
