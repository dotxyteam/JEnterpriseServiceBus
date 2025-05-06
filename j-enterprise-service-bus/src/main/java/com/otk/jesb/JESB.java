package com.otk.jesb;

import javax.swing.SwingUtilities;

import com.otk.jesb.activity.builtin.JDBCQueryActivity;
import com.otk.jesb.activity.builtin.WriteFileActivity;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.solution.Folder;
import com.otk.jesb.solution.LoopCompositeStep;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Reference;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Transition;
import com.otk.jesb.ui.GUI;

public class JESB {

	public static final boolean DEBUG = Boolean
			.valueOf(System.getProperty(JESB.class.getPackage().getName() + ".DEBUG", Boolean.FALSE.toString()));

	public static void main(String[] args) throws Exception {
		Folder plansFolder = new Folder("plans");
		Solution.INSTANCE.getContents().add(plansFolder);

		Folder otheResourcesFolder = new Folder("resources");
		Solution.INSTANCE.getContents().add(otheResourcesFolder);

		Plan plan = new Plan("test");
		plansFolder.getContents().add(plan);

		JDBCConnection c = new JDBCConnection("db");
		c.setDriverClassName("org.hsqldb.jdbcDriver");
		c.setUrl("jdbc:hsqldb:file:/tmp/db;shutdown=true;hsqldb.write_delay=false;");
		otheResourcesFolder.getContents().add(c);

		Step s1 = new Step(null);
		plan.getSteps().add(s1);
		s1.setName("a");
		s1.setDiagramX(100);
		s1.setDiagramY(100);
		JDBCQueryActivity.Builder ab1 = new JDBCQueryActivity.Builder();
		s1.setActivityBuilder(ab1);
		ab1.setConnectionReference(Reference.get(c));
		ab1.setStatement("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES");

		LoopCompositeStep ls = new LoopCompositeStep();
		plan.getSteps().add(ls);
		ls.setName("loop");
		ls.setDiagramX(200);
		ls.setDiagramY(100);
		ls.getActivityBuilder().setIterationIndexVariableName("index");
		ls.getActivityBuilder().setLoopEndCondition(new InstantiationFunction("return index==3;"));

		Step s2 = new Step(null);
		plan.getSteps().add(s2);
		s2.setName("w");
		s2.setDiagramX(300);
		s2.setDiagramY(100);
		s2.setParent(ls);
		WriteFileActivity.Builder ab2 = new WriteFileActivity.Builder();
		s2.setActivityBuilder(ab2);
		((InstanceBuilder) ((ParameterInitializer) ab2.getInstanceBuilder().getRootInitializer()).getParameterValue())
				.getParameterInitializers().add(new ParameterInitializer(0, "tmp/test.txt"));
		((InstanceBuilder) ((ParameterInitializer) ab2.getInstanceBuilder().getRootInitializer()).getParameterValue())
				.getParameterInitializers().add(new ParameterInitializer(1, new InstantiationFunction(
						"return (String)a.getRows().get(index).getCellValues().get(\"TABLE_NAME\");")));

		Transition t1 = new Transition();
		t1.setStartStep(s1);
		t1.setEndStep(ls);
		plan.getTransitions().add(t1);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				GUI.INSTANCE.openObjectFrame(Solution.INSTANCE);
			}
		});
	}

}
