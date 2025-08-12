package com.otk.jesb;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.operation.builtin.JDBCQuery;
import com.otk.jesb.operation.builtin.WriteFile;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Folder;
import com.otk.jesb.solution.LoopCompositeStep;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Transition;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.ui.Preferences;

public class JESB {

	public static final boolean DEBUG = Boolean
			.valueOf(System.getProperty(JESB.class.getPackage().getName() + ".debug", Boolean.FALSE.toString()));
	private static final boolean RUNNER_LOG_VERBOSE = Boolean.valueOf(
			System.getProperty(JESB.class.getPackage().getName() + ".runnerLogVerbose", Boolean.FALSE.toString()));

	public static final String RUNNER_SWITCH_ARGUMENT = "run-solution";
	public static final String ENVIRONMENT_SETTINGS_OPTION_ARGUMENT = "env-settings";

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(Option.builder().longOpt(RUNNER_SWITCH_ARGUMENT).desc("Only execute the solution").get());
		options.addOption(Option.builder().longOpt(ENVIRONMENT_SETTINGS_OPTION_ARGUMENT).hasArg()
				.argName("ENVIRONMENT_SETTINGS_FILE_PATH").desc("Specify the solution environment settings file")
				.get());
		CommandLine commandLine = null;
		try {
			CommandLineParser CommandLineParser = new DefaultParser();
			commandLine = CommandLineParser.parse(options, args);
		} catch (ParseException e) {
			throw newIllegalArgumentException(e, args, options);
		}
		String[] remainingArgs = commandLine.getArgs();
		File fileOrFolder;
		if (remainingArgs.length == 1) {
			fileOrFolder = new File(remainingArgs[0]);
		} else if (remainingArgs.length == 0) {
			fileOrFolder = null;
		} else {
			throw newIllegalArgumentException(null, args, options);
		}
		LogManager runnerlogManager;
		if (commandLine.hasOption(RUNNER_SWITCH_ARGUMENT)) {
			if (fileOrFolder == null) {
				throw newIllegalArgumentException(new Exception("Missing <DIRECTORY_PATH> or <ARCHIVE_FILE_PATH>"),
						args, options);
			}
			runnerlogManager = new LogManager(new File(fileOrFolder.getName() + ".log"));
			System.setOut(runnerlogManager.interceptPrintStreamData(System.out, Console.VERBOSE_LEVEL_NAME,
					() -> RUNNER_LOG_VERBOSE));
			System.setErr(runnerlogManager.interceptPrintStreamData(System.err, Console.VERBOSE_LEVEL_NAME,
					() -> RUNNER_LOG_VERBOSE));
			System.out.println("Starting up...");
			Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Shutting down...")));
		} else {
			runnerlogManager = null;
			System.setOut(Console.DEFAULT.interceptPrintStreamData(System.out, Console.VERBOSE_LEVEL_NAME, "#009999",
					"#00FFFF", () -> Preferences.INSTANCE.isLogVerbose()));
			System.setErr(Console.DEFAULT.interceptPrintStreamData(System.err, Console.VERBOSE_LEVEL_NAME, "#009999",
					"#00FFFF", () -> Preferences.INSTANCE.isLogVerbose()));
		}
		if (fileOrFolder != null) {
			if (fileOrFolder.isDirectory()) {
				Solution.INSTANCE.loadFromDirectory(fileOrFolder);
			} else if (fileOrFolder.isFile()) {
				Solution.INSTANCE.loadFromArchiveFile(fileOrFolder);
			} else {
				throw newIllegalArgumentException(
						new IOException("Invalid solution directory or archive file: '" + fileOrFolder + "'"), args,
						options);
			}
		} else {
			if (DEBUG) {
				setupSampleSolution();
			}
		}
		if (runnerlogManager != null) {
			Runner runner = new Runner(Solution.INSTANCE, runnerlogManager);
			if (commandLine.hasOption(ENVIRONMENT_SETTINGS_OPTION_ARGUMENT)) {
				Solution.INSTANCE.getEnvironmentSettings()
						.importProperties(new File(commandLine.getOptionValue(ENVIRONMENT_SETTINGS_OPTION_ARGUMENT)));
			}
			runner.activatePlans();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					GUI.INSTANCE.openObjectFrame(Solution.INSTANCE);
				}
			});
		}
	}

	private static IllegalArgumentException newIllegalArgumentException(Exception e, String[] args, Options options) {
		HelpFormatter helpFormatter = HelpFormatter.builder().get();
		return new IllegalArgumentException("Found: " + Arrays.toString(args) + ".\n"
				+ "Expected: [[<DIRECTORY_PATH> | <ARCHIVE_FILE_PATH>] " + helpFormatter.toSyntaxOptions(options) + "]",
				e);
	}

	private static void setupSampleSolution() {
		Folder plansFolder = new Folder("plans");
		Solution.INSTANCE.getContents().add(plansFolder);

		Folder otheResourcesFolder = new Folder("resources");
		Solution.INSTANCE.getContents().add(otheResourcesFolder);

		Plan plan = new Plan("test");
		plansFolder.getContents().add(plan);

		JDBCConnection c = new JDBCConnection("db");
		c.getDriverClassNameVariant().setValue("org.hsqldb.jdbcDriver");
		c.getUrlVariant().setValue("jdbc:hsqldb:file:/tmp/db;shutdown=true;hsqldb.write_delay=false;");
		otheResourcesFolder.getContents().add(c);

		Step s1 = new Step();
		plan.getSteps().add(s1);
		s1.setName("a");
		s1.setDiagramX(100);
		s1.setDiagramY(100);
		JDBCQuery.Builder ab1 = new JDBCQuery.Builder();
		s1.setOperationBuilder(ab1);
		ab1.setConnectionReference(Reference.get(c));
		ab1.getStatementVariant().setValue("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES");

		LoopCompositeStep ls = new LoopCompositeStep();
		plan.getSteps().add(ls);
		ls.setName("loop");
		ls.setDiagramX(200);
		ls.setDiagramY(100);
		ls.getOperationBuilder().setIterationIndexVariableName("index");
		ls.getOperationBuilder().setLoopEndCondition(new Function("return index==3;"));

		Step s2 = new Step();
		plan.getSteps().add(s2);
		s2.setName("w");
		s2.setDiagramX(300);
		s2.setDiagramY(100);
		s2.setParent(ls);
		WriteFile.Builder ab2 = new WriteFile.Builder();
		s2.setOperationBuilder(ab2);
		((InstanceBuilder) ((ParameterInitializer) ab2.getInstanceBuilder().getRootInitializer()).getParameterValue())
				.getParameterInitializers().add(new ParameterInitializer(0, "tmp/test.txt"));
		((InstanceBuilder) ((ParameterInitializer) ab2.getInstanceBuilder().getRootInitializer()).getParameterValue())
				.getParameterInitializers().add(new ParameterInitializer(1,
						new InstantiationFunction("return (String)a.rows[index].cellValues.get(\"TABLE_NAME\");")));

		Transition t1 = new Transition();
		t1.setStartStep(s1);
		t1.setEndStep(ls);
		plan.getTransitions().add(t1);
	}

}
