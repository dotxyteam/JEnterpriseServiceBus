package com.otk.jesb.solution;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.MiscUtils;

public class JAR extends Asset {

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
