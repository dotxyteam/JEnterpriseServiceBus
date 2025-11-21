package com.otk.jesb;

import java.io.File;
import java.net.ConnectException;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.SolutionUtils;

import xy.ui.testing.Tester;

public class ApiTest {

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

	@Before
	public void beforeEachTest() {
		Solution.INSTANCE.getContents().clear();
		Solution.INSTANCE.getRequiredJARs().clear();
		Solution.INSTANCE.getEnvironmentSettings().getEnvironmentVariableTreeElements().clear();
	}

	@Test
	public void testRestService() throws Exception {
		File archiveFile = MiscUtils.createTemporaryFile("zip");
		try {
			MiscUtils.writeBinary(archiveFile,
					MiscUtils.readBinary(ApiTest.class.getResourceAsStream("/testWebServices/test-rest-solution.zip")),
					false);
			Solution.INSTANCE.loadFromArchiveFile(archiveFile);
			Solution.INSTANCE.validate();
			try (Session session = Session.openDummySession()) {
				Plan servicePlan = SolutionUtils.findAsset(Solution.INSTANCE, Plan.class, "greetingByName");
				SolutionUtils.activatePlan(servicePlan, session);
				Plan clientPlan = SolutionUtils.findAsset(Solution.INSTANCE, Plan.class, "testGreetingsService");
				Object result;
				String message;
				{
					result = SolutionUtils.executePlan(clientPlan, session, null);
					message = Expression.evaluateObjectMemberSelection(result, "message", String.class);
					if (!message.equals("Hello John!")) {
						Assert.fail();
					}
				}
				{
					result = SolutionUtils.executePlan(clientPlan, session, inputBuilder -> {
						InstantiationUtils.setChildInitializerValue(inputBuilder, "name", "Liza");
					});
					message = Expression.evaluateObjectMemberSelection(result, "message", String.class);
					if (!message.equals("Hello Liza!")) {
						Assert.fail();
					}
				}
				{
					result = SolutionUtils.executePlan(clientPlan, session, inputBuilder -> {
						InstantiationUtils.setChildInitializerValue(inputBuilder, "name", "");
					});
					message = Expression.evaluateObjectMemberSelection(result, "message", String.class);
					if (!message.contains("Name not provided!")) {
						Assert.fail();
					}
				}
				SolutionUtils.deactivatePlan(servicePlan);
				try {
					SolutionUtils.executePlan(clientPlan, session, null);
					Assert.fail();
				} catch (Exception e) {
					if (!MiscUtils.getPrintedStackTrace(e).contains(ConnectException.class.getSimpleName())) {
						Assert.fail();
					}
				}
			}
		} finally {
			MiscUtils.delete(archiveFile);
		}
	}

	@Test
	public void testSoapService() throws Exception {
		File archiveFile = MiscUtils.createTemporaryFile("zip");
		try {
			MiscUtils.writeBinary(archiveFile,
					MiscUtils.readBinary(ApiTest.class.getResourceAsStream("/testWebServices/test-soap-solution.zip")),
					false);
			Solution.INSTANCE.loadFromArchiveFile(archiveFile);
			Solution.INSTANCE.validate();
			try (Session session = Session.openDummySession()) {
				Plan servicePlan = SolutionUtils.findAsset(Solution.INSTANCE, Plan.class, "service");
				SolutionUtils.activatePlan(servicePlan, session);
				Plan clientPlan = SolutionUtils.findAsset(Solution.INSTANCE, Plan.class, "test");
				Object result;
				String message;
				{
					result = SolutionUtils.executePlan(clientPlan, session, inputBuilder -> {
						InstantiationUtils.setChildInitializerValue(inputBuilder, "(name)", "Liza");
					});
					message = Expression.evaluateObjectMemberSelection(result, "message", String.class);
					if (!message.equals("Hello Liza!")) {
						Assert.fail();
					}
				}
				{
					result = SolutionUtils.executePlan(clientPlan, session, inputBuilder -> {
						InstantiationUtils.setChildInitializerValue(inputBuilder, "(name)", "");
					});
					message = Expression.evaluateObjectMemberSelection(result, "message", String.class);
					if (!message.contains("Name not provided!")) {
						Assert.fail();
					}
				}
				SolutionUtils.deactivatePlan(servicePlan);
				try {
					SolutionUtils.executePlan(clientPlan, session, null);
					Assert.fail();
				} catch (Exception e) {
					if (!MiscUtils.getPrintedStackTrace(e).contains(ConnectException.class.getSimpleName())) {
						Assert.fail();
					}
				}
			}
		} finally {
			MiscUtils.delete(archiveFile);
		}
	}

}
