package com.otk.jesb.instantiation;

import java.util.LinkedHashMap;

import com.otk.jesb.Function;
import com.otk.jesb.util.MiscUtils;

public class InitializationSwitch {
	
	private LinkedHashMap<Function, InitializationCase> initializationCaseByCondition = new LinkedHashMap<Function, InitializationCase>();
	private InitializationCase defaultInitializationCase;

	public LinkedHashMap<Function, InitializationCase> getInitializationCaseByCondition() {
		return initializationCaseByCondition;
	}

	public void setInitializationCaseByCondition(LinkedHashMap<Function, InitializationCase> initializationCaseByCondition) {
		this.initializationCaseByCondition = initializationCaseByCondition;
	}

	public InitializationCase getDefaultInitializationCase() {
		return defaultInitializationCase;
	}

	public void setDefaultInitializationCase(InitializationCase defaultInitializationCase) {
		this.defaultInitializationCase = defaultInitializationCase;
	}

	
	public Function findCondition(InitializationCase initializationCase) {
		if (initializationCase == defaultInitializationCase) {
			return null;
		}
		return MiscUtils.getFirstKeyFromValue(initializationCaseByCondition, initializationCase);
	}
}