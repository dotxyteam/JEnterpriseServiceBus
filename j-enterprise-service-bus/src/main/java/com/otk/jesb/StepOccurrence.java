package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public abstract class StepOccurrence implements Variable {
	private Step step;
	private Plan plan;
	private List<Variable> variablesSnapshot;

	public StepOccurrence(Step step, Plan plan) {
		this.step = step;
		this.plan = plan;
	}

	public Step getStep() {
		return step;
	}

	public Plan getPlan() {
		return plan;
	}

	@Override
	public String getName() {
		return getStep().getName();
	}

	public List<Variable> getPostVariablesSnapshot() {
		return variablesSnapshot;
	}
	
	public void capturePostVariables(List<Variable> variables) {
		this.variablesSnapshot = new ArrayList<Variable>(variables);
	}

}
