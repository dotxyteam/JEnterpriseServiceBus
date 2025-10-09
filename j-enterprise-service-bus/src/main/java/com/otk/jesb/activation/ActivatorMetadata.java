package com.otk.jesb.activation;

import xy.reflect.ui.info.ResourcePath;

/**
 * Allows to describe a specific type of {@link Activator}.
 * 
 * @author olitank
 *
 */
public interface ActivatorMetadata {

	/**
	 * @return The path to the icon describing the specific {@link Activator} type.
	 */
	ResourcePath getActivatorIconImagePath();

	/**
	 * @return The specific {@link Activator} type.
	 */
	Class<? extends Activator> getActivatorClass();

	/**
	 * @return The display name of the specific {@link Activator} type.
	 */
	String getActivatorName();

}
