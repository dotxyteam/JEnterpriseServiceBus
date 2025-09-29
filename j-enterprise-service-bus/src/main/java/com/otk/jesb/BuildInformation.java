package com.otk.jesb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class BuildInformation {

	private static final Properties PROPERTIES = new Properties();
	static {
		try (InputStream in = BuildInformation.class.getClassLoader().getResourceAsStream("build.properties")) {
			if (in != null) {
				PROPERTIES.load(in);
			} else {
				throw new UnexpectedError();
			}
		} catch (IOException e) {
			throw new UnexpectedError();
		}
	}

	private BuildInformation() {
	}

	public static String getArtifactId() {
		return PROPERTIES.getProperty("artifactId");
	}

	public static String getGroupId() {
		return PROPERTIES.getProperty("groupId");
	}

	public static String getVersion() {
		return PROPERTIES.getProperty("version");
	}

	public static String getCompilerSourceVersion() {
		return PROPERTIES.getProperty("compiler.sourceVersion");
	}

	public static String getCompilerTargetVersion() {
		return PROPERTIES.getProperty("compiler.targetVersion");
	}

	public static String asString() {
		return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
	}
}
