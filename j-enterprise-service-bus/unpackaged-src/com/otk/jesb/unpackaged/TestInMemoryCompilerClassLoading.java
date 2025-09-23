package com.otk.jesb.unpackaged;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import com.otk.jesb.Session;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;

public class TestInMemoryCompilerClassLoading {

	public static void main(String[] args) throws Exception {
		try (URLClassLoader classLoader = new URLClassLoader(
				new URL[] { new File("./tmp/plugin-jars-comparaison/file-8326478456596934304.jar").toURI().toURL() })) {
			Solution.INSTANCE.setRequiredJARs(Collections.emptyList());
			MiscUtils.IN_MEMORY_COMPILER.setBaseClassLoader(classLoader);
			test(classLoader);
		}
	}

	private static void test(ClassLoader classLoader) throws Exception {
		Plan plan = new Plan();
		Solution.INSTANCE.getContents().add(plan);
		Session session = Session.createDummySession();
		ExecutionContext context = new ExecutionContext(session, plan);
		ExecutionInspector executionInspector = new ExecutionInspector() {

			@Override
			public void logWarning(String message) {
			}

			@Override
			public void logInformation(String message) {

			}

			@Override
			public void logError(String message) {

			}

			@Override
			public boolean isExecutionInterrupted() {
				return false;
			}

			@Override
			public void beforeOperation(StepCrossing stepCrossing) {

			}

			@Override
			public void afterOperation(StepCrossing stepCrossing) {
			}
		};
		OperationBuilder<?> testOpBuilder = (OperationBuilder<?>) classLoader.loadClass("com.otk6.TestOp$Builder")
				.newInstance();
		Operation testOp = testOpBuilder.build(context, executionInspector);
		System.out.println(testOp);
	}

}
