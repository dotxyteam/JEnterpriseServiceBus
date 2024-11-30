package com.otk.jesb;

import com.otk.jesb.Activity.Result;

public class StepOccurrence {

	private Step step;
	private Activity.Result activityResult;

	public StepOccurrence() {
	}

	public StepOccurrence(Step step, Result activityResult) {
		this.step = step;
		this.activityResult = activityResult;
	}

	public Step getStep() {
		return step;
	}

	public Activity.Result getActivityResult() {
		return activityResult;
	}

}
