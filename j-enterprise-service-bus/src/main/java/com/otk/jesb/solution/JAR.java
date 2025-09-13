package com.otk.jesb.solution;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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

	public static void configureSolutionDependencies(List<JAR> jars) {
		URLClassLoader jarsClassLoader = new URLClassLoader(
				jars.stream().map(JAR::getURL).toArray(length -> new URL[length]), Solution.class.getClassLoader());
		MiscUtils.IN_MEMORY_COMPILER.setCustomBaseClassLoader(jarsClassLoader);
		for (JAR jar : jars) {
			JarURLConnection connection;
			try {
				connection = (JarURLConnection) new URL("jar:" + jar.getURL().toString() + "!/").openConnection();
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			Manifest manifest;
			try {
				manifest = connection.getManifest();
			} catch (IOException e) {
				continue;
			}
			Attributes attributes = manifest.getMainAttributes();
			String operationMetadataClassNames = attributes
					.getValue(JAR.PLUGIN_OPERATION_METADATA_CLASSES_MANIFEST_KEY);
			String activatorMetadataClassNames = attributes
					.getValue(JAR.PLUGIN_ACTIVATOR_METADATA_CLASSES_MANIFEST_KEY);
			String resourceMetadataClassNames = attributes.getValue(JAR.PLUGIN_RESOURCE_METADATA_CLASSES_MANIFEST_KEY);
			PLUGIN_OPERATION_METADATAS.clear();
			if (operationMetadataClassNames != null) {
				for (String className : operationMetadataClassNames.split(",")) {
					if (className.isEmpty()) {
						continue;
					}
					try {
						PLUGIN_OPERATION_METADATAS
								.add((OperationMetadata<?>) MiscUtils.getJESBClass(className).newInstance());
					} catch (InstantiationException | IllegalAccessException e) {
						throw new UnexpectedError(e);
					}
				}
			}
			PLUGIN_ACTIVATOR_METADATAS.clear();
			if (activatorMetadataClassNames != null) {
				for (String className : activatorMetadataClassNames.split(",")) {
					if (className.isEmpty()) {
						continue;
					}
					try {
						PLUGIN_ACTIVATOR_METADATAS
								.add((ActivatorMetadata) MiscUtils.getJESBClass(className).newInstance());
					} catch (InstantiationException | IllegalAccessException e) {
						throw new UnexpectedError(e);
					}
				}
			}
			PLUGIN_RESOURCE_METADATAS.clear();
			if (resourceMetadataClassNames != null) {
				for (String className : resourceMetadataClassNames.split(",")) {
					if (className.isEmpty()) {
						continue;
					}
					try {
						PLUGIN_RESOURCE_METADATAS
								.add((ResourceMetadata) MiscUtils.getJESBClass(className).newInstance());
					} catch (InstantiationException | IllegalAccessException e) {
						throw new UnexpectedError(e);
					}
				}
			}
		}
	}

}
