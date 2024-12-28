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

}
