package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

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
				if(!visitAssets(assetVisitor, folderContent)) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	public Debugger createDebugger() {
		return new Debugger(this);
	}

}
