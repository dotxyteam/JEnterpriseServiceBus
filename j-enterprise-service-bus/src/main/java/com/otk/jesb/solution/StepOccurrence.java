package com.otk.jesb.solution;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.otk.jesb.Variable;

public abstract class StepOccurrence implements Variable {
	private Step step;
	private Plan plan;
	private List<Variable> variablesSnapshot;
	private long timestamp;

	public StepOccurrence(Step step, Plan plan) {
		this.step = step;
		this.plan = plan;
		this.timestamp = System.currentTimeMillis();
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

	public String getFormattedTimestamp() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timestamp));
	}
}
