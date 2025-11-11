package com.otk.jesb;

import java.io.File;

import com.otk.jesb.Runner;
import com.otk.jesb.Session;
import com.otk.jesb.Variant;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.builtin.ReadCommandLine;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;

/**
 * This is an example class that demonstrates how JESB processes can be called
 * from any Java code.
 * 
 * @author olitank
 *
 */
public class TestJesbAPI {

	public static void main(String[] args) throws Exception {
		/*
		 * Here we show how to create a solution programmatically, save/load it from the
		 * file system, and execute it (synchronously or asynchronously).
		 */
		Solution solution = generateHelloCommandSolution();
		File solutionFile = new File("hello-command-solution.zip");
		{
			solution.saveToArchiveFile(solutionFile);
			solution.loadFromArchiveFile(solutionFile);
			if(!solutionFile.delete()) {
				throw new AssertionError();
			}
		}
		if (!executeHelloPlan(solution, "John").equals("Hello John!")) {
			throw new AssertionError();
		}
		runSolution(solution);
	}

	/**
	 * @return A sample solution.
	 */
	private static Solution generateHelloCommandSolution() {
		Solution solution = Solution.INSTANCE;
		Plan plan = new Plan();
		{
			solution.getContents().add(plan);
			ReadCommandLine activator = new ReadCommandLine();
			{
				plan.setActivator(activator);
				activator.setPromptVariant(new Variant<String>(String.class, "Enter your name> "));
			}
			RootInstanceBuilderFacade intitializationRoot = plan.getOutputBuilder().getFacade();
			FieldInitializerFacade commandOutputInitializer = (FieldInitializerFacade) intitializationRoot.getChildren()
					.get(0).getChildren().get(0);
			commandOutputInitializer.setFieldValue(new InstantiationFunction(
					"return \"Hello \" + " + Plan.INPUT_VARIABLE_NAME + ".inputLine + \"!\";"));
		}
		return solution;
	}

	/**
	 * Executes synchronously the single plan of the given sample solution.
	 * 
	 * @param solution The sample solution.
	 * @param name     The sample plan parameter value.
	 * @return The result of the execution of the sample plan.
	 * @throws Exception If a problem occurs.
	 */
	private static String executeHelloPlan(Solution solution, String name) throws Exception {
		try (Session session = Session.openDummySession()) {
			Plan plan = (Plan) solution.getContents().get(0);
			Activator activator = plan.getActivator();
			String inputParameter = "John";
			Object input = activator.getInputClass().getConstructor(String.class).newInstance(inputParameter);
			Object output = plan.execute(input, ExecutionInspector.DEFAULT, new ExecutionContext(session, plan));
			String outputResult = (String) plan.getActivator().getOutputClass().getField("result").get(output);
			return outputResult;
		}
	}

	/**
	 * Executes asynchronously all the activable plans of the given sample solution.
	 * 
	 * @param solution The sample solution.
	 * @throws Exception If a problem occurs.
	 */
	private static void runSolution(Solution solution) throws Exception {
		System.out.println("Starting solution...");
		try (Runner runner = new Runner(solution)) {
			runner.open();
			Thread.sleep(10000);
		}
		System.out.println("Stopping solution...");
	}

}
