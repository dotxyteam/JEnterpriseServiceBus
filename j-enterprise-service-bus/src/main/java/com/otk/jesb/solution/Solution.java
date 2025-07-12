package com.otk.jesb.solution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.Debugger;
import com.otk.jesb.EnvironmentSettings;
import com.otk.jesb.ValidationError;
import com.otk.jesb.util.MiscUtils;

public class Solution {

	public static Solution INSTANCE = new Solution();

	private static final String FILE_NAME_SUFFIX = ".xml";

	private Folder rootFolder = new Folder("rootFolder");
	private EnvironmentSettings environmentSettings = new EnvironmentSettings();

	public EnvironmentSettings getEnvironmentSettings() {
		return environmentSettings;
	}

	public void setEnvironmentSettings(EnvironmentSettings environmentSettings) {
		this.environmentSettings = environmentSettings;
	}

	public List<Asset> getContents() {
		return rootFolder.getContents();
	}

	public void setContents(List<Asset> contents) {
		rootFolder.setContents(contents);
	}

	public void visitContents(AssetVisitor assetVisitor) {
		rootFolder.visitContents(assetVisitor);
	}

	public void validate() throws ValidationError {
		rootFolder.validate(true);
	}

	public Debugger createDebugger() {
		return new Debugger(this);
	}

	public void loadFromDirectory(File directory) throws IOException {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("'" + directory + "' is not a valid directory");
		}
		try (FileInputStream fileInputStream = new FileInputStream(
				new File(directory, "." + environmentSettings.getClass().getSimpleName() + FILE_NAME_SUFFIX))) {
			environmentSettings = (EnvironmentSettings) MiscUtils.deserialize(fileInputStream);
		}
		rootFolder = loadFolder(directory, rootFolder.getName());
	}

	private Folder loadFolder(File parentDirectory, String folderName) throws IOException {
		File folderDirectory = new File(parentDirectory, folderName);
		if (!folderDirectory.isDirectory()) {
			throw new IllegalArgumentException("'" + folderDirectory + "' is not a valid directory");
		}
		Folder folder = new Folder(folderName);
		for (String name : folderDirectory.list()) {
			folder.getContents().add(loadAsset(folderDirectory, name));
		}
		return folder;
	}

	private Asset loadAsset(File parentDirectory, String name) throws IOException {
		File fileOrDirectory = new File(parentDirectory, name);
		if (fileOrDirectory.isDirectory()) {
			return loadFolder(parentDirectory, name);
		} else {
			try (FileInputStream fileInputStream = new FileInputStream(fileOrDirectory)) {
				return (Asset) MiscUtils.deserialize(fileInputStream);
			}
		}
	}

	public void saveToDirectory(File directory) throws IOException {
		if (directory.exists()) {
			MiscUtils.delete(directory);
		}
		MiscUtils.createDirectory(directory);
		try (FileOutputStream fileOutputStream = new FileOutputStream(new File(directory,
				"." + environmentSettings.getClass().getSimpleName().toLowerCase() + FILE_NAME_SUFFIX))) {
			MiscUtils.serialize(environmentSettings, fileOutputStream);
		}
		saveFolder(directory, rootFolder);
	}

	private void saveFolder(File parentDirectory, Folder folder) throws IOException {
		File folderDirectory = new File(parentDirectory, folder.getName());
		MiscUtils.createDirectory(folderDirectory);
		for (Asset asset : folder.getContents()) {
			saveAsset(folderDirectory, asset);
		}
	}

	private void saveAsset(File parentDirectory, Asset asset) throws IOException {
		if (asset instanceof Folder) {
			saveFolder(parentDirectory, (Folder) asset);
		} else {
			try (FileOutputStream fileOutputStream = new FileOutputStream(new File(parentDirectory,
					asset.getName() + "." + asset.getClass().getSimpleName().toLowerCase() + FILE_NAME_SUFFIX))) {
				MiscUtils.serialize(asset, fileOutputStream);
			}
		}
	}

}
