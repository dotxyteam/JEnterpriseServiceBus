package com.otk.jesb.solution;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Attributes;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.MiscUtils;

public class JAR extends Asset {

	public static final Attributes.Name PLUGIN_OPERATION_METADATA_CLASSES_MANIFEST_KEY = new Attributes.Name(
			"Operation-Metadata-Classes");
	public static final Attributes.Name PLUGIN_ACTIVATOR_METADATA_CLASSES_MANIFEST_KEY = new Attributes.Name(
			"Activator-Metadata-Classes");
	public static final Attributes.Name PLUGIN_RESOURCE_METADATA_CLASSES_MANIFEST_KEY = new Attributes.Name(
			"Resource-Metadata-Classes");

	
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
