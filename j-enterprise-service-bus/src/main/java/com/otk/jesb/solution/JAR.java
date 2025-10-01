package com.otk.jesb.solution;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.MiscUtils;

public class JAR extends Asset {

	public static final Attributes.Name PLUGIN_OPERATION_METADATA_CLASSES_MANIFEST_KEY = new Attributes.Name(
			"Operation-Metadata-Classes");
	public static final Attributes.Name PLUGIN_ACTIVATOR_METADATA_CLASSES_MANIFEST_KEY = new Attributes.Name(
			"Activator-Metadata-Classes");
	public static final Attributes.Name PLUGIN_RESOURCE_METADATA_CLASSES_MANIFEST_KEY = new Attributes.Name(
			"Resource-Metadata-Classes");

	public static final List<OperationMetadata<?>> PLUGIN_OPERATION_METADATAS = new ArrayList<OperationMetadata<?>>();
	public static final List<ResourceMetadata> PLUGIN_RESOURCE_METADATAS = new ArrayList<ResourceMetadata>();
	public static final List<ActivatorMetadata> PLUGIN_ACTIVATOR_METADATAS = new ArrayList<ActivatorMetadata>();

	private File temporaryFile;

	public JAR(File file) throws IOException {
		this(file.getName(), MiscUtils.readBinary(file));
	}

	public JAR(String fileName, byte[] binaryData) {
		super(fileName);
		try {
			temporaryFile = MiscUtils.createTemporaryFile("jar");
			temporaryFile.deleteOnExit();
			MiscUtils.writeBinary(temporaryFile, binaryData, false);
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		temporaryFile.delete();
	}

	@Override
	public String getFileSystemResourceName() {
		return getName();
	}

	public URL getURL() {
		try {
			return temporaryFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new UnexpectedError(e);
		}
	}

}
