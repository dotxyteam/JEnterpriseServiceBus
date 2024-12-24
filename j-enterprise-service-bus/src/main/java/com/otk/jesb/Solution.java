package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public class Solution {

	public static Solution INSTANCE = new Solution();

	private List<Resource> contents = new ArrayList<Resource>();

	private Solution() {
	}

	public List<Resource> getContents() {
		return contents;
	}

	public void setContents(List<Resource> contents) {
		this.contents = contents;
	}

}
