package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public class Folder extends Resource {

	private List<Resource> contents = new ArrayList<Resource>();

	public Folder(String name) {
		super(name);
	}

	public List<Resource> getContents() {
		return contents;
	}

	public void setContents(List<Resource> contents) {
		this.contents = contents;
	}

}
