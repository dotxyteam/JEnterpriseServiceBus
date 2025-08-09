package com.otk.jesb.solution;

import java.beans.Transient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.Debugger;
import com.otk.jesb.EnvironmentSettings;
import com.otk.jesb.PotentialError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.operation.builtin.Evaluate;
import com.otk.jesb.util.MiscUtils;

public class Solution {

	public static Solution INSTANCE = new Solution();

	private static final String SORTED_NAMES_FILE_NAME = ".sortedNames" + MiscUtils.SERIALIZED_FILE_NAME_SUFFIX;

	private Folder rootFolder = new Folder("rootFolder");
	private EnvironmentSettings environmentSettings = new EnvironmentSettings();
	private Experiment defualtExperiment = new Experiment(new Evaluate.Builder());

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

	@Transient
	public Experiment getDefualtExperiment() {
		return defualtExperiment;
	}

	public void setDefualtExperiment(Experiment defualtExperiment) {
		this.defualtExperiment = defualtExperiment;
	}

	public void visitContents(AssetVisitor assetVisitor) {
		rootFolder.visitContents(assetVisitor);
	}

	public void validate() throws ValidationError {
		environmentSettings.validate();
		rootFolder.validate(true);
	}

	public Debugger createDebugger() {
		return new Debugger(this);
	}

	public void loadFromArchiveFile(File archiveFile) throws IOException {
		if (!archiveFile.isFile()) {
			throw new IllegalArgumentException("'" + archiveFile + "' is not a valid file");
		}
		URI uri = URI.create("jar:" + archiveFile.toURI());
		try (FileSystem zipFs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "false"))) {
			load(zipFs.getPath("/"));
		}
	}

	public void loadFromDirectory(File directory) throws IOException {
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("'" + directory + "' is not a valid directory");
		}
		load(directory.toPath());
	}

	private void load(Path rootPath) throws IOException {
		try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(
				Files.readAllBytes(rootPath.resolve("." + environmentSettings.getClass().getSimpleName().toLowerCase()
						+ MiscUtils.SERIALIZED_FILE_NAME_SUFFIX)))) {
			environmentSettings = (EnvironmentSettings) MiscUtils.deserialize(fileInputStream);
		}
		rootFolder = loadFolder(rootPath, rootFolder.getName());
	}

	private Folder loadFolder(Path parentPath, String folderName) throws IOException {
		Path folderPath = parentPath.resolve(folderName);
		if (!Files.isDirectory(folderPath)) {
			throw new IllegalArgumentException("'" + folderPath + "' is not a valid directory");
		}
		Folder folder = new Folder(folderName);
		try (Stream<Path> pathStream = Files.walk(folderPath, 1).skip(1)) {
			for (Path path : pathStream.collect(Collectors.toList())) {
				if (path.getFileName().toString().equals(SORTED_NAMES_FILE_NAME)) {
					continue;
				}
				folder.getContents().add(loadAsset(folderPath, path.getFileName().toString()));
			}
		}
		try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(
				Files.readAllBytes(folderPath.resolve(SORTED_NAMES_FILE_NAME)))) {
			@SuppressWarnings("unchecked")
			final List<String> sortedNames = (List<String>) MiscUtils.deserialize(fileInputStream);
			Collections.sort(folder.getContents(), new Comparator<Asset>() {
				@Override
				public int compare(Asset asset1, Asset asset2) {
					return new Integer(sortedNames.indexOf(asset1.getName()))
							.compareTo(new Integer(sortedNames.indexOf(asset2.getName())));
				}
			});
		} catch (IOException ignore) {
		}
		return folder;
	}

	private Asset loadAsset(Path parentPath, String name) throws IOException {
		Path fileOrDirectoryPath = parentPath.resolve(name);
		if (Files.isDirectory(fileOrDirectoryPath)) {
			return loadFolder(parentPath, name);
		} else {
			try (ByteArrayInputStream fileInputStream = new ByteArrayInputStream(
					Files.readAllBytes(fileOrDirectoryPath))) {
				return (Asset) MiscUtils.deserialize(fileInputStream);
			}
		}
	}

	public void saveToArchiveFile(File archiveFile) throws IOException {
		if (archiveFile.exists()) {
			MiscUtils.delete(archiveFile);
		}
		URI uri = URI.create("jar:" + archiveFile.toURI());
		try (FileSystem zipFs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"))) {
			save(zipFs.getPath("/"));
		}
	}

	public void saveToDirectory(File directory) throws IOException {
		if (directory.exists()) {
			MiscUtils.delete(directory);
		}
		MiscUtils.createDirectory(directory);
		save(directory.toPath());
	}

	private void save(Path rootPath) throws IOException {
		try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
			MiscUtils.serialize(environmentSettings, fileOutputStream);
			Files.write(rootPath.resolve("." + environmentSettings.getClass().getSimpleName().toLowerCase()
					+ MiscUtils.SERIALIZED_FILE_NAME_SUFFIX), fileOutputStream.toByteArray());
		}
		saveFolder(rootPath, rootFolder);
	}

	private void saveFolder(Path parentPath, Folder folder) throws IOException {
		Path folderPath = parentPath.resolve(folder.getName());
		Files.createDirectories(folderPath);
		for (Asset asset : folder.getContents()) {
			saveAsset(folderPath, asset);
		}
		try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
			Files.write(folderPath.resolve(SORTED_NAMES_FILE_NAME), fileOutputStream.toByteArray());
			List<String> sortedNames = folder.getContents().stream().map(asset -> asset.getName())
					.collect(Collectors.toList());
			MiscUtils.serialize(sortedNames, fileOutputStream);
		}
	}

	private void saveAsset(Path parentPath, Asset asset) throws IOException {
		if (asset instanceof Folder) {
			saveFolder(parentPath, (Folder) asset);
		} else {
			Path assetPath = parentPath.resolve(asset.getFileSystemResourceName());
			if (Files.exists(assetPath)) {
				throw new PotentialError("Duplicate file detected while saving: " + assetPath);
			}
			try (ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream()) {
				Files.write(assetPath, fileOutputStream.toByteArray());
				MiscUtils.serialize(asset, fileOutputStream);
			}
		}
	}

}
