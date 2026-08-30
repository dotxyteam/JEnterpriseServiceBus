package com.otk.jesb;

public class JMSPluginInfo implements com.otk.jesb.IPluginInfo {

	@Override
	public java.util.List<com.otk.jesb.operation.OperationMetadata<?>> getOperationMetadatas() {
		return java.util.Arrays.asList(
				new com.otk.jesb.operation.builtin.SendJMSMessage.Metadata()		);
	}

	@Override
	public java.util.List<com.otk.jesb.activation.ActivatorMetadata> getActivatorMetadatas() {
		return java.util.Arrays.asList(
				new com.otk.jesb.activation.builtin.ReceiveJMSMessage.Metadata()		);
	}

	@Override
	public java.util.List<com.otk.jesb.resource.ResourceMetadata> getResourceMetadatas() {
		return java.util.Arrays.asList(
				new com.otk.jesb.resource.builtin.ActiveMQConnection.Metadata()		);
	}

}
