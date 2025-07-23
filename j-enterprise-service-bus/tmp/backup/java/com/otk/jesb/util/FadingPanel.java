package com.otk.jesb.util;

import java.awt.BorderLayout;

public class FadingPanel extends TransparentPanel {
	private static final long serialVersionUID = 1L;

	int fadingLevel = 0;
	int fadingSpeedPercentage = 90;

	public FadingPanel() {
		setOpacity(1.0f);
		setLayout(new BorderLayout());
	}

	public void fade(int fadingLevelIncrement, double opacityTarget) {
		int EACH_STEP_MINIMUM_DURATION = 50;
		long startTime = System.currentTimeMillis();
		long minimumSpentTime = 0;
		while (true) {
			long spentTime = System.currentTimeMillis() - startTime;
			if ((minimumSpentTime - spentTime) > 0) {
				MiscUtils.sleepSafely(minimumSpentTime - spentTime);
			}
			double opacity = getFadingAnimationOpacity(fadingLevel, fadingSpeedPercentage);
			setOpacity((float) opacity);
			paintImmediately(getBounds());
			if (opacity == opacityTarget) {
				break;
			}
			fadingLevel = fadingLevel + fadingLevelIncrement;
			minimumSpentTime += EACH_STEP_MINIMUM_DURATION;
		}
	}

	protected double getFadingAnimationOpacity(int fadingLevel, int fadingSpeedPercentage) {
		int MIN_STEPS = 2;
		int MAX_STEPS = 12;
		final int fadingStepCount = Math
				.round(MIN_STEPS + ((MAX_STEPS - MIN_STEPS) * (100.0f - fadingSpeedPercentage) / 100.0f));
		double result = 1 - (((double) fadingLevel) / (double) fadingStepCount);
		result = Math.max(result, 0d);
		result = Math.min(result, 1d);
		return result;
	}

}