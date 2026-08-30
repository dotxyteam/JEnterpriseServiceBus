package com.otk.jesb;

public class PluginInfo implements com.otk.jesb.IPluginInfo {

	@Override
	public java.util.List<com.otk.jesb.operation.OperationMetadata<?>> getOperationMetadatas() {
		return java.util.Arrays.asList(
				new com.otk.jesb.SendJMSMessage.Metadata()		);
	}

	@Override
	public java.util.List<com.otk.jesb.activation.ActivatorMetadata> getActivatorMetadatas() {
		return java.util.Arrays.asList(
				new com.otk.jesb.ReceiveMessage.Metadata()		);
	}

	@Override
	public java.util.List<com.otk.jesb.resource.ResourceMetadata> getResourceMetadatas() {
		return java.util.Arrays.asList(
				new com.otk.jesb.JMSConnection.Metadata()		);
	}

}
