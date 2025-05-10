package com.otk.jesb.solution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.otk.jesb.solution.AssetVisitor;
import com.otk.jesb.Debugger;
import com.otk.jesb.ValidationError;
import com.otk.jesb.util.MiscUtils;

public class Solution {

	public static Solution INSTANCE = new Solution();

	private Folder rootFolder = new Folder(Solution.class.getName() + ".rootFolder");

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

	public void loadFromFile(File input) throws IOException {
		FileInputStream stream = new FileInputStream(input);
		try {
			loadFromStream(stream);
		} finally {
			try {
				stream.close();
			} catch (Exception ignore) {
			}
		}
	}

	public void loadFromStream(InputStream input) throws IOException {
		Solution loaded = (Solution) MiscUtils.deserialize(input);
		setContents(loaded.getContents());
	}

	public void saveToStream(OutputStream output) throws IOException {
		Solution toSave = new Solution();
		toSave.setContents(getContents());
		MiscUtils.serialize(toSave, output);
	}

	public void saveToFile(File output) throws IOException {
		FileOutputStream stream = new FileOutputStream(output);
		try {
			saveToStream(stream);
		} finally {
			try {
				stream.close();
			} catch (Exception ignore) {
			}
		}
	}

}
