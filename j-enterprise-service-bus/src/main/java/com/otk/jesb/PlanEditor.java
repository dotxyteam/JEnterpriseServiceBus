package com.otk.jesb;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.otk.jesb.diagram.JConnection;
import com.otk.jesb.diagram.JDiagram;
import com.otk.jesb.diagram.JDiagramListener;
import com.otk.jesb.diagram.JNode;

import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.undo.AbstractSimpleModificationListener;
import xy.reflect.ui.undo.IModification;

public class PlanEditor {

	public static void main(String[] args) throws Exception {
		JFrame frame = new JFrame();
		Plan plan = new Plan();
		Workspace.PLANS.add(plan);

		JDBCConnectionResource c = new JDBCConnectionResource();
		c.setUrl("jdbc:hsqldb:file:/tmp/db;shutdown=true;hsqldb.write_delay=false;");
		Workspace.JDBC_CONNECTIONS.add(c);

		Step s1 = new Step();
		plan.getSteps().add(s1);
		s1.setName("a");
		s1.setDiagramX(100);
		s1.setDiagramY(100);
		JDBCQueryActivity.Builder ab1 = new JDBCQueryActivity.Builder();
		s1.setActivityBuilder(ab1);
		ab1.setConnectionPath(JDBCQueryActivity.Builder.getConnectionPathChoices().get(0));
		ab1.setStatement("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES");

		Step s2 = new Step();
		plan.getSteps().add(s2);
		s2.setName("w");
		s2.setDiagramX(200);
		s2.setDiagramY(100);
		WriteFileActivity.Builder ab2 = new WriteFileActivity.Builder();
		s2.setActivityBuilder(ab2);
		ab2.getObjectSpecification().getFieldInitializers()
				.add(new InstanceSpecification.FieldInitializer("filePath", "tmp/test.txt"));
		ab2.getObjectSpecification().getFieldInitializers()
				.add(new InstanceSpecification.FieldInitializer("text",
						new InstanceSpecification.DynamicValue("" + "StringBuilder s = new StringBuilder();\n"
								+ "for(com.otk.jesb.JDBCQueryActivity.GenericResultRow row: a.getRows()){\n"
								+ "  s.append(row.getCellValues().get(\"TABLE_NAME\") + \", \");\n" + "}\n"
								+ "return s.toString();")));

		Transition t1 = new Transition();
		t1.setStartStep(s1);
		t1.setEndStep(s2);
		plan.getTransitions().add(t1);

		PlanEditor planEditor = new PlanEditor(plan);
		frame.getContentPane().add(planEditor.createComponent());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setVisible(true);
	}

	private Plan plan;
	private JDiagram diagram;
	private Form form;

	public PlanEditor(Plan plan) {
		this.plan = plan;
	}

	public Component createComponent() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		diagram = createDiagram();
		panel.add(diagram, BorderLayout.CENTER);
		panel.add(new JScrollPane(form = GUI.INSTANCE.createForm(plan)), BorderLayout.WEST);
		form.getModificationStack().addListener(new AbstractSimpleModificationListener() {
			@Override
			protected void handleAnyEvent(IModification modification) {
				updateDiagram();
			}
		});
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				updateDiagram();
			}
		});
		return panel;
	}

	private JDiagram createDiagram() {
		JDiagram result = new JDiagram();
		result.addListener(new JDiagramListener() {

			@Override
			public void nodeMoved(JNode node) {
				Step step = (Step) node.getObject();
				step.setDiagramX(node.getX());
				step.setDiagramY(node.getY());
				form.refresh(false);
			}

			@Override
			public void nodeSelected(JNode node) {
				Step step = (Step) node.getObject();
				ListControl stepsControl = (ListControl) form.getFieldControlPlaceHolder("steps").getFieldControl();
				stepsControl.setSingleSelection(stepsControl.getRootListItemPosition(plan.getSteps().indexOf(step)));
			}

			@Override
			public void connectionAdded(JConnection conn) {
				Transition newTransition = new Transition();
				newTransition.setStartStep((Step) conn.getStartNode().getObject());
				newTransition.setEndStep((Step) conn.getEndNode().getObject());
				plan.getTransitions().add(newTransition);
				form.refresh(false);
			}
		});
		return result;
	}

	protected void updateDiagram() {
		diagram.clear();
		for (Step step : plan.getSteps()) {
			diagram.addNode(step, step.getDiagramX(), step.getDiagramY());
		}
		for (Transition t : plan.getTransitions()) {
			JNode node1 = diagram.getNode(t.getStartStep());
			JNode node2 = diagram.getNode(t.getEndStep());
			diagram.addConnection(node1, node2);
		}
		diagram.repaint();
	}

}
