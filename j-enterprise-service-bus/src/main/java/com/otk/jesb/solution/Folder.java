package com.otk.jesb.solution;

import java.util.ArrayList;
import java.util.List;

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

}
