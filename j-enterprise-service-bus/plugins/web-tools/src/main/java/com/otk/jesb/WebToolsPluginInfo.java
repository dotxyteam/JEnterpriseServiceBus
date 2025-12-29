package com.otk.jesb;

import java.util.Arrays;
import java.util.List;

import com.otk.jesb.IPluginInfo;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.ResourceMetadata;

public class WebToolsPluginInfo implements IPluginInfo {

	@Override
	public List<OperationMetadata<?>> getOperationMetadatas() {
		return Arrays.asList(new com.otk.jesb.operation.builtin.CallRESTAPI.Metadata(),
				new com.otk.jesb.operation.builtin.CallSOAPWebService.Metadata(),
				new com.otk.jesb.operation.builtin.GenerateXML.Metadata(),
				new com.otk.jesb.operation.builtin.ParseXML.Metadata());
	}

	@Override
	public List<ActivatorMetadata> getActivatorMetadatas() {
		return Arrays.asList(new com.otk.jesb.activation.builtin.ReceiveRESTRequest.Metadata(),
				new com.otk.jesb.activation.builtin.ReceiveSOAPRequest.Metadata());
	}

	@Override
	public List<ResourceMetadata> getResourceMetadatas() {
		return Arrays.asList(new com.otk.jesb.resource.builtin.HTTPServer.Metadata(),
				new com.otk.jesb.resource.builtin.OpenAPIDescription.Metadata(),
				new com.otk.jesb.resource.builtin.WSDL.Metadata(), new com.otk.jesb.resource.builtin.XSD.Metadata());
	}

}
