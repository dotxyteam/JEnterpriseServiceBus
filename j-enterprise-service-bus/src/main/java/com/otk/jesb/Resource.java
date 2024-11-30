package com.otk.jesb;

public class Resource implements FolderContent {

	private String name;

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
