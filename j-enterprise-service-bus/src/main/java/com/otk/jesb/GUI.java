package com.otk.jesb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeCellRenderer;

import com.otk.jesb.InstanceSpecification.DynamicValue;
import com.otk.jesb.InstanceSpecification.FacadeNode;
import com.otk.jesb.InstanceSpecification.FieldInitializerFacade;
import com.otk.jesb.InstanceSpecification.InstanceSpecificationFacade;
import com.otk.jesb.InstanceSpecification.ListItemInitializerFacade;
import com.otk.jesb.InstanceSpecification.ParameterInitializerFacade;
import com.otk.jesb.InstanceSpecification.ValueMode;
import com.otk.jesb.diagram.JConnection;
import com.otk.jesb.diagram.JDiagram;
import com.otk.jesb.diagram.JDiagramListener;
import com.otk.jesb.diagram.JNode;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.control.DefaultFieldControlData;
import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.NullableControl;
import xy.reflect.ui.control.swing.TextControl;
import xy.reflect.ui.control.swing.customizer.CustomizingFieldControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.customizer.CustomizingMethodControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.ControlPanel;
import xy.reflect.ui.control.swing.util.ControlScrollPane;
import xy.reflect.ui.control.swing.util.ControlSplitPane;
import xy.reflect.ui.control.swing.util.ScrollPaneOptions;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.field.CapsuleFieldInfo;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.filter.IInfoFilter;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.parameter.ParameterInfoProxy;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.info.type.iterable.item.ItemPositionFactory;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.undo.AbstractSimpleModificationListener;
import xy.reflect.ui.undo.IModification;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.undo.UndoOrder;
import xy.reflect.ui.util.Accessor;
import xy.reflect.ui.util.Listener;
import xy.reflect.ui.util.PrecomputedTypeInstanceWrapper;
import xy.reflect.ui.util.ReflectionUIUtils;

public class GUI extends SwingCustomizer {

	public static void main(String[] args) throws Exception {
		Folder plansFolder = new Folder("plans");
		Solution.INSTANCE.getContents().add(plansFolder);

		Folder otheResourcesFolder = new Folder("resources");
		Solution.INSTANCE.getContents().add(otheResourcesFolder);

		Plan plan = new Plan("test");
		plansFolder.getContents().add(plan);

		JDBCConnectionResource c = new JDBCConnectionResource("db");
		c.setUrl("jdbc:hsqldb:file:/tmp/db;shutdown=true;hsqldb.write_delay=false;");
		otheResourcesFolder.getContents().add(c);

		Step s1 = new Step();
		plan.getSteps().add(s1);
		s1.setName("a");
		s1.setDiagramX(100);
		s1.setDiagramY(100);
		JDBCQueryActivity.Builder ab1 = new JDBCQueryActivity.Builder();
		s1.setActivityBuilder(ab1);
		ab1.setConnection(c);
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

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				GUI.INSTANCE.openObjectFrame(Solution.INSTANCE);
			}
		});
	}

	private static final String GUI_CUSTOMIZATIONS_RESOURCE_DIRECTORY = System
			.getProperty(PlanEditor.class.getPackage().getName() + ".alternateUICustomizationsFileDirectory");
	private static final String GUI_CUSTOMIZATIONS_RESOURCE_NAME = "jesb.icu";

	public static GUI INSTANCE = new GUI();

	private GUI() {
		super(new Reflecter());
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
			return new CustomizingForm(this, object, infoFilter) {

				private static final long serialVersionUID = 1L;

				@Override
				protected CustomizingFieldControlPlaceHolder createFieldControlPlaceHolder(IFieldInfo field) {
					return new CustomizingFieldControlPlaceHolder(this, field) {

						private static final long serialVersionUID = 1L;

						@Override
						public Component createFieldControl() {
							if (object instanceof InstanceSpecificationFacade) {
								if (field.getName().equals("children")) {
									return new InstanceSpecificationControl(GUI.this, this);
								}
							}
							if (object instanceof PrecomputedTypeInstanceWrapper) {
								Object instance = ((PrecomputedTypeInstanceWrapper) object).getInstance();
								if (instance instanceof CapsuleFieldInfo.Value) {
									Object encapsulated = ((CapsuleFieldInfo.Value) instance).getObject();
									if (encapsulated instanceof FieldInitializerFacade) {
										if (field.getName().equals("fieldValue")) {
											return new NullableControl(this.swingRenderer, this) {

												private static final long serialVersionUID = 1L;

												@Override
												protected Object getNewValue() {
													FieldInitializerFacade facade = (FieldInitializerFacade) ((CapsuleFieldInfo.Value) ((PrecomputedTypeInstanceWrapper) getObject())
															.getInstance()).getObject();
													if ((facade.getFieldValueMode() == null)
															|| (facade.getFieldValueMode() == ValueMode.STATIC_VALUE)) {
														return swingRenderer.onTypeInstantiationRequest(this,
																facade.getFieldInfo().getType());
													} else {
														return super.getNewValue();
													}
												}

											};
										}
									}
									if (encapsulated instanceof ParameterInitializerFacade) {
										if (field.getName().equals("parameterValue")) {
											return new NullableControl(this.swingRenderer, this) {

												private static final long serialVersionUID = 1L;

												@Override
												protected Object getNewValue() {
													ParameterInitializerFacade facade = (ParameterInitializerFacade) ((CapsuleFieldInfo.Value) ((PrecomputedTypeInstanceWrapper) getObject())
															.getInstance()).getObject();
													if (facade.getParameterValueMode() == ValueMode.STATIC_VALUE) {
														return swingRenderer.onTypeInstantiationRequest(this,
																facade.getParameterInfo().getType());
													} else {
														return super.getNewValue();
													}
												}

											};
										}
									}
									if (encapsulated instanceof ListItemInitializerFacade) {
										if (field.getName().equals("itemValue")) {
											return new NullableControl(this.swingRenderer, this) {

												private static final long serialVersionUID = 1L;

												@Override
												protected Object getNewValue() {
													ListItemInitializerFacade facade = (ListItemInitializerFacade) ((CapsuleFieldInfo.Value) ((PrecomputedTypeInstanceWrapper) getObject())
															.getInstance()).getObject();
													if ((facade.getItemValueMode() == null)
															|| (facade.getItemValueMode() == ValueMode.STATIC_VALUE)) {
														return swingRenderer.onTypeInstantiationRequest(this,
																facade.getItemType());
													} else {
														return super.getNewValue();
													}
												}

											};
										}
									}
								}
							}
							return super.createFieldControl();
						}

						@Override
						public void refreshUI(boolean recreate) {
							setVisible(true);
							if (object instanceof InstanceSpecificationFacade) {
								if (field.getName().equals("selectedConstructorSignature")) {
									setBorder(BorderFactory.createTitledBorder(field.getCaption()));
									Object[] valueOptions = field.getValueOptions(object);
									if (valueOptions != null) {
										if (valueOptions.length <= 1) {
											setVisible(false);
										}
									}
								}
							}
							super.refreshUI(recreate);
						}

						@Override
						public boolean showsCaption() {
							if (object instanceof InstanceSpecificationFacade) {
								if (field.getName().equals("selectedConstructorSignature")) {
									return true;
								}
							}
							return super.showsCaption();
						}

					};
				}

				@Override
				protected CustomizingMethodControlPlaceHolder createMethodControlPlaceHolder(IMethodInfo method) {
					if (object instanceof ExpressionEditor) {
						if (method.getName().equals("insertSelectedPathNodeExpression")) {
							method = new MethodInfoProxy(method) {

								@Override
								public List<IParameterInfo> getParameters() {
									return Collections.emptyList();
								}

								@Override
								public Object invoke(Object object, InvocationData invocationData) {
									TextControl textControl = (TextControl) getFieldControlPlaceHolder("expression")
											.getFieldControl();
									invocationData.getProvidedParameterValues().put(0,
											textControl.getTextComponent().getSelectionStart());
									invocationData.getProvidedParameterValues().put(1,
											textControl.getTextComponent().getSelectionEnd());
									return super.invoke(object, invocationData);
								}

							};
						}
					}
					return super.createMethodControlPlaceHolder(method);
				}

			};
		}
	}

	public static class InstanceSpecificationControl extends ListControl {

		private static final long serialVersionUID = 1L;

		public InstanceSpecificationControl(SwingRenderer swingRenderer, IFieldControlInput input) {
			super(swingRenderer, input);
		}

		@Override
		protected TreeCellRenderer createTreeCellRenderer() {
			return new ItemTreeCellRenderer() {

				@Override
				protected void customizeCellRendererComponent(JLabel label, ItemNode node, int rowIndex,
						int columnIndex, boolean isSelected, boolean hasFocus) {
					super.customizeCellRendererComponent(label, node, rowIndex, columnIndex, isSelected, hasFocus);
					BufferedItemPosition itemPosition = getItemPositionByNode(node);
					if (itemPosition != null) {
						if (itemPosition.getItem() instanceof FacadeNode) {
							FacadeNode facadeNode = (FacadeNode) itemPosition.getItem();
							if (!facadeNode.isConcrete()) {
								label.setForeground(Color.LIGHT_GRAY);
							}
						}
					}
				}

			};
		}

	}

	public static class Reflecter extends CustomizedUI {

		public static final List<ActivityMetadata> ACTIVITY_METADATAS = Arrays.asList(new JDBCQueryActivity.Metadata(),
				new WriteFileActivity.Metadata());
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
												return new ExpressionEditor(((DynamicValue) object).getScript(),
														currentPlan, currentStep);
											}

										});
							}

							@Override
							public Object invoke(Object object, InvocationData invocationData) {
								ExpressionEditor expressionEditor = (ExpressionEditor) invocationData
										.getParameterValue(0);
								((DynamicValue) object).setScript(expressionEditor.getExpression());
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
						for (ActivityMetadata activityMetadata : ACTIVITY_METADATAS) {
							result.add(getTypeInfo(new JavaTypeInfoSource(Reflecter.this,
									activityMetadata.getActivityBuilderClass(), null)));
						}
						return result;
					} else {
						return super.getPolymorphicInstanceSubTypes(type);
					}
				}

				@Override
				protected String getCaption(ITypeInfo type) {
					for (ActivityMetadata activityMetadata : ACTIVITY_METADATAS) {
						if (activityMetadata.getActivityBuilderClass().getName().equals(type.getName())) {
							return activityMetadata.getActivityTypeName();
						}
					}
					return super.getCaption(type);
				}

				@Override
				protected ResourcePath getIconImagePath(ITypeInfo type, Object object) {
					for (ActivityMetadata activityMetadata : ACTIVITY_METADATAS) {
						if (activityMetadata.getActivityBuilderClass().getName().equals(type.getName())) {
							return activityMetadata.getActivityIconImagePath();
						}
					}
					if (type.getName().equals(Step.class.getName())) {
						return MiscUtils.getIconImagePath((Step) object);
					}
					return super.getIconImagePath(type, object);
				}

			}.wrapTypeInfo(super.getTypeInfoBeforeCustomizations(type));
		}

	}

	public static class PlanEditor extends CustomizingForm {

		private static final long serialVersionUID = 1L;
		private JDiagram diagram;
		private boolean selectionListeningEnabled = true;
		private boolean modificationListeningEnabled = true;
		private ControlSplitPane splitPane;

		public PlanEditor(SwingCustomizer swingRenderer, Plan plan, IInfoFilter infoFilter) {
			super(swingRenderer, plan, infoFilter);
			getModificationStack().addListener(new AbstractSimpleModificationListener() {
				@Override
				protected void handleAnyEvent(IModification modification) {
					if (modificationListeningEnabled) {
						updateDiagram();
					}
				}
			});
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					updateDiagram();
				}
			});
		}

		public Plan getPlan() {
			return (Plan) object;
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
			splitPane = new ControlSplitPane();
			{
				container.add(splitPane, BorderLayout.CENTER);
				splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
				{
					splitPane.setLeftComponent(new ControlScrollPane(diagram));
				}
				ControlPanel membersPanel = new ControlPanel();
				{
					splitPane
							.setRightComponent(new ControlScrollPane(new ScrollPaneOptions(membersPanel, true, false)));
					super.layoutMembersPanels(membersPanel, fieldsPanel, methodsPanel);
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						double dividerLocation = 0.5;
						SwingRendererUtils.ensureDividerLocation(splitPane, dividerLocation);
						splitPane.setResizeWeight(dividerLocation);
					}
				});
			}
		}

		@Override
		public void refresh(boolean refreshStructure) {
			super.refresh(refreshStructure);
			if (refreshStructure) {
				if (swingRenderer.getReflectionUI().getApplicationInfo().getMainBorderColor() != null) {
					splitPane.setBorder(BorderFactory.createLineBorder(SwingRendererUtils
							.getColor(swingRenderer.getReflectionUI().getApplicationInfo().getMainBorderColor())));
				} else {
					splitPane.setBorder(new JSplitPane().getBorder());
				}
				updateDiagram();
			}
		}

		private JDiagram createDiagram() {
			JDiagram result = new JDiagram();
			result.addListener(new JDiagramListener() {

				@Override
				public void nodeMoved(JNode node) {
					Step step = (Step) node.getObject();
					modificationListeningEnabled = false;
					try {
						ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
						ITypeInfo stepType = reflectionUI
								.getTypeInfo(new JavaTypeInfoSource(reflectionUI, Step.class, null));
						getModificationStack().insideComposite("Change Step Position", UndoOrder.getNormal(),
								new Accessor<Boolean>() {
									@Override
									public Boolean get() {
										ReflectionUIUtils.setFieldValueThroughModificationStack(
												new DefaultFieldControlData(reflectionUI, step,
														ReflectionUIUtils.findInfoByName(stepType.getFields(),
																"diagramX")),
												node.getX(), getModificationStack(),
												ReflectionUIUtils.getDebugLogListener(reflectionUI));
										ReflectionUIUtils.setFieldValueThroughModificationStack(
												new DefaultFieldControlData(reflectionUI, step,
														ReflectionUIUtils.findInfoByName(stepType.getFields(),
																"diagramY")),
												node.getY(), getModificationStack(),
												ReflectionUIUtils.getDebugLogListener(reflectionUI));
										return true;
									}
								}, false);
					} finally {
						modificationListeningEnabled = true;
					}
				}

				@Override
				public void nodeSelected(JNode node) {
					if (selectionListeningEnabled) {
						selectionListeningEnabled = false;
						try {
							if (node == null) {
								getStepsControl().setSingleSelection(null);
							} else {
								Step step = (Step) node.getObject();
								ListControl stepsControl = getStepsControl();
								getStepsControl().setSingleSelection(
										stepsControl.getRootListItemPosition(getPlan().getSteps().indexOf(step)));
							}
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
					try {
						ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
						ITypeInfo planType = reflectionUI
								.getTypeInfo(new JavaTypeInfoSource(reflectionUI, Plan.class, null));
						modificationListeningEnabled = false;
						DefaultFieldControlData transitionsData = new DefaultFieldControlData(reflectionUI, getPlan(),
								ReflectionUIUtils.findInfoByName(planType.getFields(), "transitions"));
						IModification modification = new ListModificationFactory(
								new ItemPositionFactory(transitionsData).getRootItemPosition(-1)).add(0, newTransition);
						getModificationStack().apply(modification);
					} finally {
						modificationListeningEnabled = true;
					}
				}
			});
			result.setBackground(Color.WHITE);
			return result;
		}

		private void updateDiagram() {
			if (diagram == null) {
				return;
			}
			diagram.clear();
			for (Step step : getPlan().getSteps()) {
				JNode node = diagram.addNode(step, step.getDiagramX(), step.getDiagramY());
				ResourcePath iconImagePath = MiscUtils.getIconImagePath(step);
				if (iconImagePath != null) {
					node.setImage(SwingRendererUtils.loadImageThroughCache(iconImagePath,
							ReflectionUIUtils.getDebugLogListener(swingRenderer.getReflectionUI())));
				}
			}
			for (Transition t : getPlan().getTransitions()) {
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
