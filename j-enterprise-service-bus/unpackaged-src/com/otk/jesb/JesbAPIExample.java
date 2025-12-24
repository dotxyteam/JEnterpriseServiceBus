package com.otk.jesb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import com.otk.jesb.Runner;
import com.otk.jesb.Session;
import com.otk.jesb.Variant;
import com.otk.jesb.activation.builtin.ReadCommandLine;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.SolutionUtils;
import com.otk.jesb.solution.Solution;

/**
 * This is an example class that demonstrates how JESB processes can be called
 * from any Java code.
 * 
 * @author olitank
 *
 */
public class JesbAPIExample {

	public static void main(String[] args) throws Exception {
		/*
		 * Here we show how to create a solution programmatically, save/load it from the
		 * file system, and execute it.
		 */
		Solution solution = generateHelloCommandSolution();
		File solutionFile = new File("hello-command-solution.zip");
		{
			solution.saveToArchiveFile(solutionFile);
			solution.loadFromArchiveFile(solutionFile);
			if (!solutionFile.delete()) {
				throw new AssertionError();
			}
		}
		if (!executeHelloPlan(solution, "John").equals("Hello John!")) {
			throw new AssertionError();
		}
		if (!triggerHelloPlan(solution, "John").contains("Hello John!")) {
			throw new AssertionError();
		}
		runSolution(solution);
	}

	/**
	 * @return The example solution.
	 */
	private static Solution generateHelloCommandSolution() {
		Solution solution = new Solution();
		Plan plan = new Plan();
		{
			solution.getContents().add(plan);
			ReadCommandLine activator = new ReadCommandLine();
			{
				plan.setActivator(activator);
				activator.setPromptVariant(new Variant<String>(String.class, "Enter your name> "));
			}
			RootInstanceBuilderFacade intitializationRoot = plan.getOutputBuilder().getFacade(solution);
			FieldInitializerFacade commandOutputInitializer = (FieldInitializerFacade) intitializationRoot.getChildren()
					.get(0).getChildren().get(0);
			commandOutputInitializer.setFieldValue(new InstantiationFunction(
					"return \"Hello \" + " + plan.getInputVariableName() + ".inputLine + \"!\";"));
		}
		return solution;
	}

	/**
	 * Synchronously executes the plan of the given example solution.
	 * 
	 * @param solution       The example solution.
	 * @param parameterValue The value of the the example plan parameter.
	 * @return The result of the example plan execution.
	 * @throws Exception Thrown in case of a problem.
	 */
	private static String executeHelloPlan(Solution solution, String parameterValue) throws Exception {
		try (Session session = Session.openDummySession(solution)) {
			Plan plan = (Plan) SolutionUtils.findAsset(solution, Plan.class::isInstance);
			Object output = SolutionUtils.executePlan(plan, session, inputBuilder -> {
				InstantiationUtils.setChildInitializerValue(inputBuilder, "(inputLine)", "John", solution);
			});
			return Expression.evaluateObjectMemberSelection(output, "result", String.class, solution);
		}
	}

	/**
	 * Activates the plan of the given example solution, triggers it, and
	 * deactivates it.
	 * 
	 * @param solution       The example solution.
	 * @param parameterValue The value of the the example plan parameter.
	 * @return The result of the example plan execution.
	 * @throws Exception Thrown in case of a problem.
	 */
	private static String triggerHelloPlan(Solution solution, String parameterValue) throws Exception {
		try (Session session = Session.openDummySession(solution)) {
			Plan plan = (Plan) SolutionUtils.findAsset(solution, Plan.class::isInstance);
			SolutionUtils.activatePlan(plan, session);
			try {
				PrintStream initialStandardOutput = JESB.getStandardOutput();
				try {
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					JESB.setStandardOutput(new PrintStream(buffer));
					JESB.getStandardInputSource().pushLine(parameterValue);
					Thread.sleep(5000);
					return buffer.toString();
				} finally {
					JESB.setStandardOutput(initialStandardOutput);
				}
			} finally {
				SolutionUtils.deactivatePlan(plan, session);
			}
		}
	}

	/**
	 * Starts the given example solution, waits for a certain period of time and
	 * stops it.
	 * 
	 * @param solution The example solution.
	 * @throws Exception If a problem occurs.
	 */
	private static void runSolution(Solution solution) throws Exception {
		System.out.println("Starting solution...");
		try (Runner runner = new Runner(solution)) {
			Thread.sleep(10000);
		}
		System.out.println("Stopping solution...");
	}

}
