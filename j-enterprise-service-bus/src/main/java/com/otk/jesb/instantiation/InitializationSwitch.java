package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows to specify multiple conditional exclusive instantiation cases.
 * 
 * @author olitank
 *
 */
public class InitializationSwitch {

	private List<CaseEntry> caseEntries = new ArrayList<CaseEntry>();
	private InitializationCase defaultInitializationCase;

	public List<CaseEntry> getCaseEntries() {
		return caseEntries;
	}

	public void setCaseEntries(List<CaseEntry> caseEntries) {
		this.caseEntries = caseEntries;
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
		return caseEntries.stream().filter(entry -> entry.getInitializationCase() == initializationCase)
				.map(CaseEntry::getCondition).findFirst().orElse(null);
	}

	public void insertCase(int index, InstantiationFunction condition, InitializationCase initializationCase) {
		caseEntries.add(index, new CaseEntry(condition, initializationCase));
	}

	public void addCase(InstantiationFunction condition, InitializationCase initializationCase) {
		insertCase(caseEntries.size(), condition, initializationCase);
	}

	public InitializationCase findCase(InstantiationFunction condition) {
		return caseEntries.stream().filter(entry -> (entry.getCondition() == condition))
				.map(CaseEntry::getInitializationCase).findFirst().orElse(null);
	}

	public boolean removeCase(InstantiationFunction condition) {
		return caseEntries.removeIf(entry -> (entry.getCondition() == condition));
	}

	public static class CaseEntry {
		private InstantiationFunction condition;
		private InitializationCase initializationCase;

		public CaseEntry() {
			this(null, null);
		}

		public CaseEntry(InstantiationFunction condition, InitializationCase initializationCase) {
			this.condition = condition;
			this.initializationCase = initializationCase;
		}

		public InstantiationFunction getCondition() {
			return condition;
		}

		public void setCondition(InstantiationFunction condition) {
			this.condition = condition;
		}

		public InitializationCase getInitializationCase() {
			return initializationCase;
		}

		public void setInitializationCase(InitializationCase initializationCase) {
			this.initializationCase = initializationCase;
		}

	}
}