package com.otk.jesb;

import java.io.File;
import java.util.Locale;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.MiscUtils;
import xy.ui.testing.Tester;

public class ApiTests {

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
	public void testSwitchCase() throws Exception {
		File archiveFile = MiscUtils.createTemporaryFile("zip");
		try {
			MiscUtils.writeBinary(archiveFile,
					MiscUtils.readBinary(ApiTests.class.getResourceAsStream("/testMisc/test-switch-case.zip")), false);
			Solution solutionInstance = new Solution();
			solutionInstance.loadFromArchiveFile(archiveFile);
			solutionInstance.validate();
			try (Debugger session = new Debugger(solutionInstance, true)) {
				session.activatePlans();
				Thread.sleep(3000);
				if (session.getActivePlanExecutors().size() != 0) {
					Assert.fail();
				}
				JESB.getStandardInputSource()
						.pushLine(System.getProperty("os.name").toLowerCase().contains("win") ? "dir" : "ls");
				long sleepStartTime = System.currentTimeMillis();
				while (Console.DEFAULT.getPendingInputLineConsumer() == Console.DEFAULT
						.getDefaultPendingInputLineConsumer()) {
					long sleepTimeMilliseconds = System.currentTimeMillis() - sleepStartTime;
					if (sleepTimeMilliseconds > 10000) {
						Assert.fail();
					}
					Thread.sleep(1000);
				}
				if (session.getActivePlanExecutors().size() != 1) {
					Assert.fail();
				}
				Console.DEFAULT.setPendingInputLine("no");
				Console.DEFAULT.submitPendingInputLine();
				Thread.sleep(5000);
				if (!session.getPlanActivations().stream()
						.flatMap(planActivation -> planActivation.getPlanExecutors().stream())
						.allMatch(planExecutor -> planExecutor.toString().equals("DONE"))) {
					Assert.fail();
				}
			}
		} finally {
			MiscUtils.delete(archiveFile);
		}
	}

}
