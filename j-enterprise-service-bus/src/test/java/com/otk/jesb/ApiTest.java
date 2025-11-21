package com.otk.jesb;

import java.io.File;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.otk.jesb.solution.Solution;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.control.swing.plugin.FileBrowserPlugin;
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
		checkSystemProperty(GUI.GUI_CUSTOMIZATIONS_RESOURCE_DIRECTORY_PROPERTY_KEY, null);
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
		GUI.INSTANCE.getLastInvocationDataByMethodContext().clear();
		GUI.INSTANCE.getSubCustomizerByIdentifier().values()
				.forEach(swingCustomizer -> swingCustomizer.getLastInvocationDataByMethodContext().clear());
		FileBrowserPlugin.setLastDirectory(FileBrowserPlugin.DEFAULT_DIRECTORY);
	}

	@Test
	public void testSoapService() throws Exception {
		File archiveFile = MiscUtils.createTemporaryFile("zip");
		try {
			MiscUtils.write(archiveFile,
					MiscUtils.read(ApiTest.class.getResourceAsStream("/testWebServices/test-soap-solution.zip")),
					false);
			Solution.INSTANCE.loadFromArchiveFile(archiveFile);
			Solution.INSTANCE.validate();
			Runner runner = new Runner(Solution.INSTANCE);
			runner.open();
			try {
				if (runner.getPlanActivations().size() != 1) {
					Assert.fail();
				}
				if (runner.getPlanActivations().get(0).getPlanExecutors().size() != 1) {
					Assert.fail();
				}
			} finally {
				runner.close();
			}
		} finally {
			MiscUtils.delete(archiveFile);
		}
	}

}
