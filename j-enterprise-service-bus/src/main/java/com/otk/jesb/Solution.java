package com.otk.jesb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.util.MiscUtils;

public class Solution {

	public static Solution INSTANCE = new Solution();

	private List<Asset> contents = new ArrayList<Asset>();

	private Solution() {
	}

	public List<Asset> getContents() {
		return contents;
	}

	public void setContents(List<Asset> contents) {
		this.contents = contents;
	}

	public void visitAssets(AssetVisitor assetVisitor) {
		for (Asset asset : contents) {
			if (!visitAssets(assetVisitor, asset)) {
				return;
			}
		}
	}

	private boolean visitAssets(AssetVisitor assetVisitor, Asset asset) {
		if (!assetVisitor.visitAsset(asset)) {
			return false;
		}
		if (asset instanceof Folder) {
			for (Asset folderContent : ((Folder) asset).getContents()) {
				if (!visitAssets(assetVisitor, folderContent)) {
					return false;
				}
			}
		}
		return true;
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
		contents = loaded.contents;
	}

	public void saveToStream(OutputStream output) throws IOException {
		Solution toSave = new Solution();
		toSave.contents = contents;
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
