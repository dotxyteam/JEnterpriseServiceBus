package com.otk.jesb;

public class Transition {

	private Step startStep;
	private Step endStep;

	public Step getStartStep() {
		return startStep;
	}

	public void setStartStep(Step startStep) {
		this.startStep = startStep;
	}

	public Step getEndStep() {
		return endStep;
	}

	public void setEndStep(Step endStep) {
		this.endStep = endStep;
	}
	

	@Override
	public String toString() {
		return "<Transition>";
	}

}
