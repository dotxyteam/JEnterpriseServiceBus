package com.otk.jesb;

import com.otk.jesb.util.MiscUtils;

public class Console {

	public String getOutput() {
		return MiscUtils.getPrintedStackTrace(new Exception());
	}

}
