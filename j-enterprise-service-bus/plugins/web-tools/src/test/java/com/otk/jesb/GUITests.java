package com.otk.jesb;

import java.io.File;
import java.util.Locale;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.otk.jesb.ui.GUI;

import xy.reflect.ui.control.swing.plugin.FileBrowserPlugin;
import xy.reflect.ui.util.MiscUtils;
import xy.ui.testing.Tester;
import xy.ui.testing.util.TestingUtils;

public class GUITests {

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
		checkSystemProperty(GUI.GUI_CUSTOMIZATIONS_RESOURCE_DIRECTORIES_PROPERTY_KEY, null);
		Locale.setDefault(Locale.US);
	}

	@BeforeClass
	public static void beforeAllTests() {
		setupTestEnvironment();
	}

	@Before
	public void beforeEachTest() {
		JESB.UI.INSTANCE.getSolutionInstance().getContents().clear();
		JESB.UI.INSTANCE.getSolutionInstance().getRequiredJARs().clear();
		JESB.UI.INSTANCE.getSolutionInstance().getEnvironmentSettings().getEnvironmentVariableTreeElements().clear();
		JESB.UI.INSTANCE.getLastInvocationDataByMethodContext().clear();
		JESB.UI.INSTANCE.getSubCustomizerByIdentifier().values()
				.forEach(swingCustomizer -> swingCustomizer.getLastInvocationDataByMethodContext().clear());
		FileBrowserPlugin.setLastDirectory(FileBrowserPlugin.DEFAULT_DIRECTORY);
	}

	@Test
	public void firstTest() throws Exception {
		TestingUtils.assertSuccessfulReplay(tester, new File("test-specifications/firstTest.stt"));
	}

}
