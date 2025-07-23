package com.otk.jesb.solution;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.ValidationError;
import com.otk.jesb.util.MiscUtils;

public class Folder extends Asset {

	private List<Asset> contents = new ArrayList<Asset>();

	public Folder() {
		this(Folder.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public Folder(String name) {
		super(name);
	}

	public List<Asset> getContents() {
		return contents;
	}

	public void setContents(List<Asset> contents) {
		this.contents = contents;
	}

	public void visitContents(AssetVisitor assetVisitor) {
		for (Asset asset : contents) {
			if (!visitAsset(assetVisitor, asset)) {
				return;
			}
		}
	}

	private boolean visitAsset(AssetVisitor assetVisitor, Asset asset) {
		if (!assetVisitor.visitAsset(asset)) {
			return false;
		}
		if (asset instanceof Folder) {
			((Folder) asset).visitContents(assetVisitor);
		}
		return true;
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		if (recursively) {
			for (Asset asset : contents) {
				try {
					asset.validate(true);
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate '" + asset.getName() + "'", e);
				}
			}
		}
	}
}
