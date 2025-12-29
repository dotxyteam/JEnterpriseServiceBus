package com.otk.jesb;

import java.util.List;

import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.ResourceMetadata;

/**
 * This class allows the implementation of JESB plugin entry points. Each
 * instance lists the metadata of the solution elements provided by a plugin.
 * 
 * @author olitank
 *
 */
public interface IPluginInfo {

	/**
	 * @return The list of {@link OperationMetadata}s provided by the plugin.
	 */
	List<OperationMetadata<?>> getOperationMetadatas();

	/**
	 * @return The list of {@link ActivatorMetadata}s provided by the plugin.
	 */
	List<ActivatorMetadata> getActivatorMetadatas();

	/**
	 * @return The list of {@link ResourceMetadata}s provided by the plugin.
	 */
	List<ResourceMetadata> getResourceMetadatas();

}
