package com.otk.jesb.instantiation;

import java.util.LinkedHashMap;
import java.util.Map;

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

	protected ParameterInitializer getDynamicParameterInitializer(EvaluationContext context, int parameterPosition,
			String parameterTypeName) {
		for (Map.Entry<Function, InitializationCase> caseEntry : initializationCaseByCondition.entrySet()) {
			boolean caseConditionFullfilled;
			try {
				caseConditionFullfilled = MiscUtils.isConditionFullfilled(caseEntry.getKey(), context);
			} catch (Exception e) {
				throw new AssertionError(e);
			}
			if (caseConditionFullfilled) {
				return caseEntry.getValue().getDynamicParameterInitializer(context, parameterPosition,
						parameterTypeName);
			}
		}
		return null;
	}

	public Function findCondition(InitializationCase initializationCase) {
		if (initializationCase == defaultInitializationCase) {
			return null;
		}
		return MiscUtils.getFirstKeyFromValue(initializationCaseByCondition, initializationCase);
	}
}