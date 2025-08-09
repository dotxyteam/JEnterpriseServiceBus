package com.otk.jesb;

import java.io.File;
import javax.swing.SwingUtilities;

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

	public static void main(String[] args) throws Exception {
		String RUNNER_SWITCH_ARGUMENT = "--run-solution";
		if ((args.length == 2) && args[0].equals(RUNNER_SWITCH_ARGUMENT)) {
			File fileOrFolder = new File(args[1]);
			if (fileOrFolder.isDirectory()) {
				Solution.INSTANCE.loadFromDirectory(fileOrFolder);
			} else if (fileOrFolder.isFile()) {
				Solution.INSTANCE.loadFromArchiveFile(fileOrFolder);
			} else {
				throw new IllegalArgumentException(
						"Invalid solution directory or archive file: '" + fileOrFolder + "'");
			}
			LogManager log = new LogManager(new File(fileOrFolder.getName() + ".log"));
			System.setOut(log.interceptPrintStreamData(System.out, Console.VERBOSE_LEVEL_NAME,
					() -> RUNNER_LOG_VERBOSE));
			System.setErr(log.interceptPrintStreamData(System.err, Console.VERBOSE_LEVEL_NAME,
					() -> RUNNER_LOG_VERBOSE));
			System.out.println("Starting up...");
			Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Shutting down...")));
			Runner runner = new Runner(Solution.INSTANCE, log);
			runner.activatePlans();
		} else if (args.length <= 1) {
			System.setOut(Console.DEFAULT.interceptPrintStreamData(System.out, Console.VERBOSE_LEVEL_NAME, "#009999",
					"#00FFFF", () -> Preferences.INSTANCE.isLogVerbose()));
			System.setErr(Console.DEFAULT.interceptPrintStreamData(System.err, Console.VERBOSE_LEVEL_NAME, "#009999",
					"#00FFFF", () -> Preferences.INSTANCE.isLogVerbose()));
			if (args.length == 1) {
				Solution.INSTANCE.loadFromDirectory(new File(args[0]));
			} else {
				if (DEBUG) {
					setupSampleSolution();
				}
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					GUI.INSTANCE.openObjectFrame(Solution.INSTANCE);
				}
			});
		} else {
			throw new IllegalArgumentException(
					"Expected: [" + RUNNER_SWITCH_ARGUMENT + "] [DIRECTORY_PATH | ARCHIVE_FILE_PATH]");
		}
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
