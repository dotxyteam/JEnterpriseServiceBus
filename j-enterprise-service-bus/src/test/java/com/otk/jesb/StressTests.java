package com.otk.jesb;

import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.otk.jesb.activation.builtin.Schedule;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Transition;
import com.otk.jesb.util.MiscUtils;
import xy.ui.testing.Tester;

public class StressTests {

	Tester tester = new Tester();

	protected static void checkSystemProperty(String key, String expectedValue) {
		String value = System.getProperty(key);
		if (!MiscUtils.equalsOrBothNull(expectedValue, value)) {
			String errorMsg = "System property invalid value:\n" + "-D" + key + "=" + value + "\nExpected:\n" + "-D"
					+ key + "=" + expectedValue;
			System.err.println(errorMsg);
			throw new AssertionError(errorMsg);

		}
	}

	public static void setupTestEnvironment() {
		checkSystemProperty(JESB.DEBUG_PROPERTY_KEY, null);
		Locale.setDefault(Locale.US);
	}

	@BeforeClass
	public static void beforeAllTests() {
		setupTestEnvironment();
	}

	@Test
	public void testMemoryLeaks() throws Exception {
		final Long PERIOD_MILLISECONDS = 10l;
		final Long SLEEP_MILLISECONDS = 100l;
		Solution solutionInstance = new Solution();
		Plan plan = new Plan();
		{
			solutionInstance.getContents().add(plan);
			Schedule activator = new Schedule();
			{
				plan.setActivator(activator);
				activator.getRepetitionSettings().getPeriodUnitVariant().setConstantValue(
						Schedule.RepetitionSettingsStructure.RepetitionSettingsStructurePeriodUnitStructure.MILLISECONDS);
				activator.getRepetitionSettings().getPeriodLengthVariant().setConstantValue(PERIOD_MILLISECONDS);
			}
			com.otk.jesb.operation.builtin.Log.Builder logOperationBuilder = new com.otk.jesb.operation.builtin.Log.Builder();
			{
				plan.getSteps().add(new Step(logOperationBuilder));
				RootInstanceBuilderFacade intitializationRoot = logOperationBuilder.getInstanceBuilder()
						.getFacade(solutionInstance);
				ParameterInitializerFacade logMessageInitializer = (ParameterInitializerFacade) intitializationRoot
						.getChildren().get(0).getChildren().get(0);
				logMessageInitializer.setParameterValue(new InstantiationFunction("return \"Activated at \" + "
						+ plan.getInputVariableName() + ".activationMoment.toString() + \"!\";"));
			}
			com.otk.jesb.operation.builtin.Sleep.Builder sleepOperationBuilder = new com.otk.jesb.operation.builtin.Sleep.Builder();
			{
				plan.getSteps().add(new Step(sleepOperationBuilder));
				RootInstanceBuilderFacade intitializationRoot = sleepOperationBuilder.getInstanceBuilder()
						.getFacade(solutionInstance);
				ParameterInitializerFacade sleepMillisecondsInitializer = (ParameterInitializerFacade) intitializationRoot
						.getChildren().get(0).getChildren().get(0);
				sleepMillisecondsInitializer.setParameterValue(SLEEP_MILLISECONDS);
			}
			Transition logToSleep = new Transition();
			{
				plan.getTransitions().add(logToSleep);
				logToSleep.setStartStep(plan.getSteps().stream().filter(
						step -> step.getOperationBuilder() instanceof com.otk.jesb.operation.builtin.Log.Builder)
						.findFirst().get());
				if (logToSleep.getStartStep() == null) {
					Assert.fail();
				}
				logToSleep.setEndStep(plan.getSteps().stream().filter(
						step -> step.getOperationBuilder() instanceof com.otk.jesb.operation.builtin.Sleep.Builder)
						.findFirst().get());
				if (logToSleep.getEndStep() == null) {
					Assert.fail();
				}

			}
		}
		boolean memoryStabilizationDetected = false;
		try (Runner session = new Runner(solutionInstance)) {
			long freeMemory = 0;
			long lastFreeMemory = Long.MAX_VALUE;
			for (int i = 0; i < 5; i++) {
				System.gc();
				freeMemory = Runtime.getRuntime().freeMemory();
				if (freeMemory >= lastFreeMemory) {
					memoryStabilizationDetected = true;
					break;
				}
				lastFreeMemory = freeMemory;
				System.err.println("Free memory: " + freeMemory);
				Thread.sleep(5000);
				int parallelPlanExecutors = session.getActivePlanExecutors().size();
				System.err.println("Parallel plan executors: " + parallelPlanExecutors);
				if (parallelPlanExecutors < Math
						.round(((float) SLEEP_MILLISECONDS / (float) PERIOD_MILLISECONDS) / 20f)) {
					Assert.fail();
				}
			}
		}
		if (!memoryStabilizationDetected) {
			Assert.fail();
		}
	}

}
