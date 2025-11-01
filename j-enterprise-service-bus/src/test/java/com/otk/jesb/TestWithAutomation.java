package com.otk.jesb;

import java.io.File;
import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;

import com.otk.jesb.ui.GUI;

import xy.reflect.ui.util.IOUtils;
import xy.reflect.ui.util.MiscUtils;
import xy.ui.testing.Tester;
import xy.ui.testing.util.TestingUtils;

public class TestWithAutomation {

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
		checkSystemProperty(GUI.GUI_CUSTOMIZATIONS_RESOURCE_DIRECTORY_PROPERTY_KEY, null);
		Locale.setDefault(Locale.US);
	}

	@BeforeClass
	public static void beforeAllTests() {
		setupTestEnvironment();
	}

	@Test
	public void testFirstUse() throws Exception {
		File solutionDirectory = new File("tmp/testFirstUse-solution/");
		if (solutionDirectory.exists()) {
			IOUtils.delete(solutionDirectory);
		}
		TestingUtils.assertSuccessfulReplay(tester, new File("test-specifications/testFirstUse.stt"));
	}

	@Test
	public void testBuiltInElements() throws Exception {
		TestingUtils.assertSuccessfulReplay(tester, new File("test-specifications/testBuiltInElements.stt"));
	}

	@Test
	public void testPluginBuilder() throws Exception {
		TestingUtils.assertSuccessfulReplay(tester, new File("test-specifications/testPluginBuilder.stt"));
	}

}
