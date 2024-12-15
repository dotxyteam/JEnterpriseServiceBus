package com.otk.jesb;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.otk.jesb.InstanceSpecification.DynamicValue;
import com.otk.jesb.diagram.JConnection;
import com.otk.jesb.diagram.JDiagram;
import com.otk.jesb.diagram.JDiagramListener;
import com.otk.jesb.diagram.JNode;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;
import xy.reflect.ui.control.swing.util.ControlPanel;
import xy.reflect.ui.control.swing.util.ControlSplitPane;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.filter.IInfoFilter;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.parameter.ParameterInfoProxy;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.undo.AbstractSimpleModificationListener;
import xy.reflect.ui.undo.IModification;
import xy.reflect.ui.util.Listener;

public class GUI extends SwingCustomizer {

	public static void main(String[] args) throws Exception {
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

		GUI.INSTANCE.openObjectFrame(plan);
	}

	private static final String GUI_CUSTOMIZATIONS_RESOURCE_DIRECTORY = System
			.getProperty(PlanEditor.class.getPackageName() + ".alternateUICustomizationsFileDirectory");
	private static final String GUI_CUSTOMIZATIONS_RESOURCE_NAME = "jesb.icu";

	public static GUI INSTANCE = new GUI();

	private GUI() {
		super(new Reflection());
		if (GUI_CUSTOMIZATIONS_RESOURCE_DIRECTORY != null) {
			setInfoCustomizationsOutputFilePath(
					GUI_CUSTOMIZATIONS_RESOURCE_DIRECTORY + "/" + GUI_CUSTOMIZATIONS_RESOURCE_NAME);
		} else {
			try {
				getInfoCustomizations()
						.loadFromStream(getClass().getResourceAsStream("/" + GUI_CUSTOMIZATIONS_RESOURCE_NAME), null);
			} catch (IOException e) {
				throw new AssertionError(e);
			}
		}
	}

	@Override
	public CustomizingForm createForm(final Object object, IInfoFilter infoFilter) {
		if (object instanceof Plan) {
			return new PlanEditor(this, (Plan) object, infoFilter);
		} else {
			return super.createForm(object, infoFilter);
		}
	}

	public static class Reflection extends CustomizedUI {

		protected static final List<Class<? extends ActivityBuilder>> ACTIVITY_BUILDER_CLASSES = Arrays
				.asList(JDBCQueryActivity.Builder.class, WriteFileActivity.Builder.class);
		private Plan currentPlan;
		private Step currentStep;

		@Override
		protected ITypeInfo getTypeInfoBeforeCustomizations(ITypeInfo type) {
			return new InfoProxyFactory() {

				@Override
				protected boolean onFormVisibilityChange(ITypeInfo type, Object object, boolean visible) {
					if (visible) {
						if (object instanceof Plan) {
							currentPlan = (Plan) object;
						} else if (object instanceof Step) {
							currentStep = (Step) object;
						}
					}
					return super.onFormVisibilityChange(type, object, visible);
				}

				@Override
				protected List<IMethodInfo> getMethods(ITypeInfo type) {
					if (type.getName().equals(DynamicValue.class.getName())) {
						List<IMethodInfo> result = new ArrayList<IMethodInfo>(super.getMethods(type));
						result.add(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

							@Override
							public String getCaption() {
								return "Assist";
							}

							@Override
							public List<IParameterInfo> getParameters() {
								return Collections
										.singletonList(new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

											@Override
											public String getCaption() {
												return "";
											}

											@Override
											public Object getDefaultValue(Object object) {
												return new PathNodeSelector(currentPlan, currentStep);
											}

										});
							}

							@Override
							public Object invoke(Object object, InvocationData invocationData) {
								PathNodeSelector pathNodeSelector = (PathNodeSelector) invocationData
										.getParameterValue(0);
								((DynamicValue) object).setScript(
										"return " + pathNodeSelector.getSelectedPathNode().generateExpression() + ";");
								return null;
							}
						});
						return result;
					} else {
						return super.getMethods(type);
					}
				}

				@Override
				protected List<ITypeInfo> getPolymorphicInstanceSubTypes(ITypeInfo type) {
					if (type.getName().equals(ActivityBuilder.class.getName())) {
						List<ITypeInfo> result = new ArrayList<ITypeInfo>();
						for (Class<?> clazz : ACTIVITY_BUILDER_CLASSES) {
							result.add(getTypeInfo(new JavaTypeInfoSource(Reflection.this, clazz, null)));
						}
						return result;
					} else {
						return super.getPolymorphicInstanceSubTypes(type);
					}
				}

			}.wrapTypeInfo(super.getTypeInfoBeforeCustomizations(type));
		}

	}

	public static class PlanEditor extends CustomizingForm {

		private static final long serialVersionUID = 1L;
		private Plan plan;
		private JDiagram diagram;
		private boolean selectionListeningEnabled = true;

		public PlanEditor(SwingCustomizer swingRenderer, Plan plan, IInfoFilter infoFilter) {
			super(swingRenderer, plan, infoFilter);
			this.plan = plan;
			getModificationStack().addListener(new AbstractSimpleModificationListener() {
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
		}

		@Override
		protected void createMembersControls() {
			super.createMembersControls();
			diagram = createDiagram();
			getStepsControl().addListControlSelectionListener(new Listener<List<BufferedItemPosition>>() {

				@Override
				public void handle(List<BufferedItemPosition> event) {
					if (selectionListeningEnabled) {
						selectionListeningEnabled = false;
						try {
							updateDiagram();
						} finally {
							selectionListeningEnabled = true;
						}
					}
				}
			});
		}

		private ListControl getStepsControl() {
			return (ListControl) getFieldControlPlaceHolder("steps").getFieldControl();
		}

		@Override
		protected void layoutMembersPanels(Container container, Container fieldsPanel, Container methodsPanel) {
			container.setLayout(new BorderLayout());
			JSplitPane newContainer = new ControlSplitPane();
			{
				container.add(newContainer, BorderLayout.CENTER);
				newContainer.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
				newContainer.setLeftComponent(diagram);
				ControlPanel membersPanel = new ControlPanel();
				{
					newContainer.setRightComponent(membersPanel);
					super.layoutMembersPanels(membersPanel, fieldsPanel, methodsPanel);
				}
				double dividerLocation = 0.7;
				SwingRendererUtils.setSafelyDividerLocation(newContainer, dividerLocation);
				newContainer.setResizeWeight(dividerLocation);
			}
		}

		JDiagram createDiagram() {
			JDiagram result = new JDiagram();
			result.addListener(new JDiagramListener() {

				@Override
				public void nodeMoved(JNode node) {
					Step step = (Step) node.getObject();
					step.setDiagramX(node.getX());
					step.setDiagramY(node.getY());
					refresh(false);
				}

				@Override
				public void nodeSelected(JNode node) {
					if (selectionListeningEnabled) {
						selectionListeningEnabled = false;
						try {
							Step step = (Step) node.getObject();
							ListControl stepsControl = getStepsControl();
							getStepsControl().setSingleSelection(
									stepsControl.getRootListItemPosition(plan.getSteps().indexOf(step)));
						} finally {
							selectionListeningEnabled = true;
						}
					}
				}

				@Override
				public void connectionAdded(JConnection conn) {
					Transition newTransition = new Transition();
					newTransition.setStartStep((Step) conn.getStartNode().getObject());
					newTransition.setEndStep((Step) conn.getEndNode().getObject());
					plan.getTransitions().add(newTransition);
					refresh(false);
				}
			});
			return result;
		}

		void updateDiagram() {
			if (diagram == null) {
				return;
			}
			diagram.clear();
			for (Step step : plan.getSteps()) {
				diagram.addNode(step, step.getDiagramX(), step.getDiagramY());
			}
			for (Transition t : plan.getTransitions()) {
				JNode node1 = diagram.getNode(t.getStartStep());
				JNode node2 = diagram.getNode(t.getEndStep());
				diagram.addConnection(node1, node2);
			}
			ListControl stepsControl = getStepsControl();
			BufferedItemPosition selection = stepsControl.getSingleSelection();
			if (selection != null) {
				diagram.select(diagram.getNode(selection.getItem()));
			} else {
				diagram.select(null);
			}
			diagram.repaint();
		}

	};

}
