package com.otk.jesb.instantiation;

import java.util.LinkedHashMap;

import com.otk.jesb.util.MiscUtils;

/**
 * Allows to specify multiple conditional exclusive instantiation cases.
 * 
 * @author olitank
 *
 */
public class InitializationSwitch {

	private LinkedHashMap<InstantiationFunction, InitializationCase> initializationCaseByCondition = new LinkedHashMap<InstantiationFunction, InitializationCase>();
	private InitializationCase defaultInitializationCase;

	public LinkedHashMap<InstantiationFunction, InitializationCase> getInitializationCaseByCondition() {
		return initializationCaseByCondition;
	}

	public void setInitializationCaseByCondition(
			LinkedHashMap<InstantiationFunction, InitializationCase> initializationCaseByCondition) {
		this.initializationCaseByCondition = initializationCaseByCondition;
	}

	public InitializationCase getDefaultInitializationCase() {
		return defaultInitializationCase;
	}

	public void setDefaultInitializationCase(InitializationCase defaultInitializationCase) {
		this.defaultInitializationCase = defaultInitializationCase;
	}

	public InstantiationFunction findCondition(InitializationCase initializationCase) {
		if (initializationCase == defaultInitializationCase) {
			return null;
		}
		return MiscUtils.getFirstKeyFromValue(initializationCaseByCondition, initializationCase);
	}
}