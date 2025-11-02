package com.otk.jesb;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Profile {

	private static final String SYSTEM_INITIALIZATION_FILE_NAME = "system.ini";
	public static final Profile INSTANCE = new Profile();
	static {
		try {
			Properties initializationProperties = new Properties();
			File systemIniFile = new File(SYSTEM_INITIALIZATION_FILE_NAME);
			if (systemIniFile.exists()) {
				initializationProperties.load(new FileReader(SYSTEM_INITIALIZATION_FILE_NAME));
			}
			String profileDirectoryPath = initializationProperties.getProperty("profileDirectoryPath", ".jesb");
			INSTANCE.profileDirectory = new File(profileDirectoryPath);
			if (!INSTANCE.profileDirectory.exists()) {
				if (!INSTANCE.profileDirectory.mkdir()) {
					throw new IOException("Failed to create the directory '" + INSTANCE.profileDirectory + "'");
				}
			}
		} catch (Throwable e) {
			Log.get().error(e);
			System.exit(-1);
		}
	}
	private File profileDirectory;

	private Profile() {
	}

	public File getProfileDirectory() {
		return profileDirectory;
	}

	public void setProfileDirectory(File profileDirectory) {
		this.profileDirectory = profileDirectory;
	}

}
