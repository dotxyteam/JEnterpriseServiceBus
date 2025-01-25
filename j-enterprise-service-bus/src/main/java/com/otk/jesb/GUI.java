package com.otk.jesb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellRenderer;

import com.otk.jesb.Debugger.PlanActivator;
import com.otk.jesb.Debugger.PlanExecutor;
import com.otk.jesb.InstanceBuilder.Function;
import com.otk.jesb.InstanceBuilder.Facade;
import com.otk.jesb.InstanceBuilder.FieldInitializerFacade;
import com.otk.jesb.InstanceBuilder.InstanceBuilderFacade;
import com.otk.jesb.InstanceBuilder.ListItemInitializerFacade;
import com.otk.jesb.InstanceBuilder.ParameterInitializerFacade;
import com.otk.jesb.InstanceBuilder.ValueMode;
import com.otk.jesb.Structure.Element;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.activity.builtin.ExecutePlanActivity;
import com.otk.jesb.activity.builtin.JDBCQueryActivity;
import com.otk.jesb.activity.builtin.JDBCUpdateActivity;
import com.otk.jesb.activity.builtin.ReadFileActivity;
import com.otk.jesb.activity.builtin.SleepActivity;
import com.otk.jesb.activity.builtin.WriteFileActivity;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.diagram.JConnection;
import com.otk.jesb.diagram.JDiagram;
import com.otk.jesb.diagram.JDiagramAction;
import com.otk.jesb.diagram.JDiagramActionCategory;
import com.otk.jesb.diagram.JDiagramActionScheme;
import com.otk.jesb.diagram.JDiagramListener;
import com.otk.jesb.diagram.JNode;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.SquigglePainter;

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
import xy.reflect.ui.control.swing.renderer.Form;
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
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationItemInfo;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.info.type.iterable.item.ItemPosition;
import xy.reflect.ui.info.type.iterable.item.ItemPositionFactory;
import xy.reflect.ui.info.type.iterable.util.DynamicListActionProxy;
import xy.reflect.ui.info.type.iterable.util.IDynamicListAction;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.undo.IModification;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.undo.UndoOrder;
import xy.reflect.ui.util.Accessor;
import xy.reflect.ui.util.Listener;
import xy.reflect.ui.util.Mapper;
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
		ab1.setConnection(c);
		ab1.setStatement("SELECT * FROM INFORMATION_SCHEMA.SYSTEM_TABLES");

		Step s2 = new Step(null);
		plan.getSteps().add(s2);
		s2.setName("w");
		s2.setDiagramX(200);
		s2.setDiagramY(100);
		WriteFileActivity.Builder ab2 = new WriteFileActivity.Builder();
		s2.setActivityBuilder(ab2);
		ab2.getInstanceBuilder().getFieldInitializers()
				.add(new InstanceBuilder.FieldInitializer("filePath", "tmp/test.txt"));
		ab2.getInstanceBuilder().getFieldInitializers().add(new InstanceBuilder.FieldInitializer("text",
				new InstanceBuilder.Function("return a.getRows().get(0).getCellValues().get(\"TABLE_NAME\");")));

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
			.getProperty(GUI.class.getPackage().getName() + ".alternateUICustomizationsFileDirectory");
	private static final String GUI_CUSTOMIZATIONS_RESOURCE_NAME = "jesb.icu";

	public static GUI INSTANCE = new GUI();

	private GUI() {
		super(new JESBReflectionUI());
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
		} else if (object instanceof PlanExecutor) {
			return new PlanExecutorView(this, (PlanExecutor) object, infoFilter);
		} else {
			return new CustomizingForm(this, object, infoFilter) {

				private static final long serialVersionUID = 1L;

				CustomizingForm thisForm = this;

				@Override
				protected CustomizingFieldControlPlaceHolder createFieldControlPlaceHolder(IFieldInfo field) {
					return new CustomizingFieldControlPlaceHolder(this, field) {

						private static final long serialVersionUID = 1L;

						@Override
						public Component createFieldControl() {
							if (object instanceof InstanceBuilderFacade) {
								if (field.getName().equals("children")) {
									return new InstanceBuilderControl(GUI.this, this);
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
															|| (facade.getFieldValueMode() == ValueMode.PLAIN)) {
														return MiscUtils.getDefaultInterpretableValue(
																facade.getFieldInfo().getType(), facade);
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
													if (facade.getParameterValueMode() == ValueMode.PLAIN) {
														return MiscUtils.getDefaultInterpretableValue(
																facade.getParameterInfo().getType(), facade);
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
															|| (facade.getItemValueMode() == ValueMode.PLAIN)) {
														return MiscUtils.getDefaultInterpretableValue(
																facade.getItemType(), facade);
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
							if (object instanceof InstanceBuilderFacade) {
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
							if (object instanceof InstanceBuilderFacade) {
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
					if (object instanceof FunctionEditor) {
						if (method.getName().equals("insertSelectedPathNodeExpression")) {
							method = new MethodInfoProxy(method) {

								@Override
								public List<IParameterInfo> getParameters() {
									return Collections.emptyList();
								}

								@Override
								public Object invoke(Object object, InvocationData invocationData) {
									TextControl textControl = (TextControl) getFieldControlPlaceHolder("functionBody")
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
					if (object instanceof PlanActivator) {
						if (method.getName().equals("executePlan")) {
							method = new MethodInfoProxy(method) {

								@Override
								public Object invoke(final Object object, InvocationData invocationData) {
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											Form debuggerForm = SwingRendererUtils.findAncestorFormOfType(thisForm,
													Debugger.class.getName(), swingRenderer);
											ListControl planActivatorsControl = (ListControl) debuggerForm
													.getFieldControlPlaceHolder("planActivators").getFieldControl();
											planActivatorsControl.refreshUI(false);
											PlanActivator currentPlanActivator = (PlanActivator) object;
											BufferedItemPosition planActivatorPosition = planActivatorsControl
													.findItemPositionByReference(currentPlanActivator);
											BufferedItemPosition lastPlanExecutorPosition = planActivatorPosition
													.getSubItemPositions()
													.get(planActivatorPosition.getSubItemPositions().size() - 1);
											planActivatorsControl.setSingleSelection(lastPlanExecutorPosition);
										}
									});
									return super.invoke(object, invocationData);
								}

							};
						}
					}
					return super.createMethodControlPlaceHolder(method);
				}

				@Override
				public void validateForm() throws Exception {
					if (object instanceof FunctionEditor) {
						TextControl textControl = (TextControl) getFieldControlPlaceHolder("functionBody")
								.getFieldControl();
						JTextComponent textComponent = textControl.getTextComponent();
						textComponent.getHighlighter().removeAllHighlights();
						try {
							((FunctionEditor) object).validateExpression();
						} catch (CompilationError e) {
							if (textComponent.getText() != null) {
								textComponent.getHighlighter().addHighlight(
										(e.getStartPosition() == -1) ? 0 : e.getStartPosition(),
										(e.getEndPosition() == -1) ? textComponent.getText().length()
												: e.getEndPosition(),
										new SquigglePainter(Color.RED));
							}
							throw e;
						}
					} else {
						super.validateForm();
					}
				}

			};
		}
	}

	@Override
	public Image getObjectIconImage(Object object) {
		Image result = super.getObjectIconImage(object);
		if (result == null) {
			return result;
		}
		return SwingRendererUtils.scalePreservingRatio(result, 16, 16, Image.SCALE_SMOOTH);
	}

	@Override
	public Image getEnumerationItemIconImage(IEnumerationItemInfo itemInfo) {
		Image result = super.getEnumerationItemIconImage(itemInfo);
		if (result == null) {
			return result;
		}
		return SwingRendererUtils.scalePreservingRatio(result, 16, 16, Image.SCALE_SMOOTH);
	}

	public static class InstanceBuilderControl extends ListControl {

		private static final long serialVersionUID = 1L;

		public InstanceBuilderControl(SwingRenderer swingRenderer, IFieldControlInput input) {
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
						if (itemPosition.getItem() instanceof Facade) {
							Facade facade = (Facade) itemPosition.getItem();
							if (!facade.isConcrete()) {
								label.setForeground(Color.LIGHT_GRAY);
							}
						}
					}
				}

			};
		}

	}

	public static class JESBReflectionUI extends CustomizedUI {

		public static final List<ActivityMetadata> ACTIVITY_METADATAS = Arrays.asList(new SleepActivity.Metadata(),
				new ExecutePlanActivity.Metadata(), new ReadFileActivity.Metadata(), new WriteFileActivity.Metadata(),
				new JDBCQueryActivity.Metadata(), new JDBCUpdateActivity.Metadata());
		public static final List<ResourceMetadata> RESOURCE_METADATAS = Arrays.asList(new JDBCConnection.Metadata());
		private Plan currentPlan;
		private Step currentStep;

		@Override
		protected ITypeInfo getTypeInfoBeforeCustomizations(ITypeInfo type) {
			return new InfoProxyFactory() {

				@Override
				protected Runnable getNextInvocationUndoJob(IMethodInfo method, ITypeInfo objectType,
						final Object object, InvocationData invocationData) {
					if (object instanceof FunctionEditor) {
						if (method.getName().equals("insertSelectedPathNodeExpression")) {
							final String oldExpression = ((FunctionEditor) object).getFunctionBody();
							return new Runnable() {
								@Override
								public void run() {
									((FunctionEditor) object).setFunctionBody(oldExpression);
								}
							};
						}
					}
					return super.getNextInvocationUndoJob(method, objectType, object, invocationData);
				}

				@Override
				protected String toString(ITypeInfo type, Object object) {
					if (object instanceof ActivityMetadata) {
						return ((ActivityMetadata) object).getActivityTypeName();
					}
					return super.toString(type, object);
				}

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
					if (type.getName().equals(Function.class.getName())) {
						List<IMethodInfo> result = new ArrayList<IMethodInfo>(super.getMethods(type));
						result.add(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

							@Override
							public String getCaption() {
								return "Assist...";
							}

							@Override
							public String getOnlineHelp() {
								return "Open the Expression Editor";
							}

							@Override
							public ITypeInfo getReturnValueType() {
								return getTypeInfo(
										new JavaTypeInfoSource(JESBReflectionUI.this, FunctionEditor.class, null));
							}

							@Override
							public Object invoke(Object object, InvocationData invocationData) {
								return new FunctionEditor(currentPlan, currentStep, (Function) object);
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
							result.add(getTypeInfo(new JavaTypeInfoSource(JESBReflectionUI.this,
									activityMetadata.getActivityBuilderClass(), null)));
						}
						return result;
					} else if (type.getName().equals(Resource.class.getName())) {
						List<ITypeInfo> result = new ArrayList<ITypeInfo>();
						for (ResourceMetadata resourceMetadata : RESOURCE_METADATAS) {
							result.add(getTypeInfo(new JavaTypeInfoSource(JESBReflectionUI.this,
									resourceMetadata.getResourceClass(), null)));
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
					for (ResourceMetadata resourceMetadata : RESOURCE_METADATAS) {
						if (resourceMetadata.getResourceClass().getName().equals(type.getName())) {
							return resourceMetadata.getResourceTypeName();
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
					for (ResourceMetadata resourceMetadata : RESOURCE_METADATAS) {
						if (resourceMetadata.getResourceClass().getName().equals(type.getName())) {
							return resourceMetadata.getResourceIconImagePath();
						}
					}
					if (object instanceof Step) {
						return MiscUtils.getIconImagePath((Step) object);
					}
					if (object instanceof StepOccurrence) {
						return MiscUtils.getIconImagePath(((StepOccurrence) object).getStep());
					}
					if (object instanceof PlanActivator) {
						return ReflectionUIUtils.getIconImagePath(JESBReflectionUI.this,
								((PlanActivator) object).getPlan());
					}
					if (object instanceof PlanExecutor) {
						PlanExecutor executor = (PlanExecutor) object;
						if (executor.isActive()) {
							return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
									PlanExecutor.class.getPackage().getName().replace(".", "/") + "/running.png"));
						} else {
							if (executor.getExecutionError() == null) {
								return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
										PlanExecutor.class.getPackage().getName().replace(".", "/") + "/success.png"));
							} else {
								return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
										PlanExecutor.class.getPackage().getName().replace(".", "/") + "/failure.png"));
							}
						}
					}
					if (object instanceof Element) {
						if (((Element) object).getOptionality() == null) {
							return getIconImagePath(getTypeInfo(new JavaTypeInfoSource(JESBReflectionUI.this,
									ParameterInitializerFacade.class, null)), null);
						} else {
							return getIconImagePath(getTypeInfo(
									new JavaTypeInfoSource(JESBReflectionUI.this, FieldInitializerFacade.class, null)),
									null);
						}
					}
					return super.getIconImagePath(type, object);
				}

			}.wrapTypeInfo(super.getTypeInfoBeforeCustomizations(type));
		}

		@Override
		protected ITypeInfo getTypeInfoAfterCustomizations(ITypeInfo type) {
			return new InfoProxyFactory() {

				@Override
				protected List<IDynamicListAction> getDynamicActions(IListTypeInfo listType,
						final List<? extends ItemPosition> selection,
						Mapper<ItemPosition, ListModificationFactory> listModificationFactoryAccessor) {
					if (listType.getItemType() != null) {
						if (listType.getItemType().getName().equals(PlanActivator.class.getName())) {
							List<IDynamicListAction> result = new ArrayList<IDynamicListAction>(
									super.getDynamicActions(listType, selection, listModificationFactoryAccessor));
							for (int i = 0; i < result.size(); i++) {
								if (result.get(i).getName().endsWith("executePlan")) {
									result.set(i, new DynamicListActionProxy(result.get(i)) {
										@Override
										public List<ItemPosition> getPostSelection() {
											List<? extends ItemPosition> subItemPositions = selection.get(0)
													.getSubItemPositions();
											return Collections
													.singletonList(subItemPositions.get(subItemPositions.size() - 1));
										}
									});
								}
							}
							return result;
						}
					}
					return super.getDynamicActions(listType, selection, listModificationFactoryAccessor);
				}
			}.wrapTypeInfo(super.getTypeInfoAfterCustomizations(type));
		}

	}

	public static class PlanEditor extends CustomizingForm {

		private static final long serialVersionUID = 1L;
		private PlanDiagram diagram;
		private boolean selectionListeningEnabled = true;
		private ControlSplitPane splitPane;

		public PlanEditor(SwingCustomizer swingRenderer, Plan plan, IInfoFilter infoFilter) {
			super(swingRenderer, plan, infoFilter);
		}

		protected void updateDiagram() {
			if (diagram == null) {
				return;
			}
			diagram.setPlan(getPlan());
			diagram.refresh();
			ListControl stepsControl = getStepsControl();
			BufferedItemPosition selection = stepsControl.getSingleSelection();
			if (selection != null) {
				diagram.select(diagram.getNode(selection.getItem()));
			} else {
				diagram.select(null);
			}
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
					JPanel diagramAndPalette = new JPanel();
					{
						splitPane.setLeftComponent(new ControlScrollPane(diagramAndPalette));
						diagramAndPalette.setLayout(new BorderLayout());
						diagramAndPalette.add(diagram, BorderLayout.CENTER);
						Component palette = diagram.createActionPalette();
						{
							palette.setPreferredSize(new Dimension(100, 100));
							diagramAndPalette.add(palette, BorderLayout.SOUTH);
						}
					}
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
			updateDiagram();
			if (refreshStructure) {
				if (swingRenderer.getReflectionUI().getApplicationInfo().getMainBorderColor() != null) {
					splitPane.setBorder(BorderFactory.createLineBorder(SwingRendererUtils
							.getColor(swingRenderer.getReflectionUI().getApplicationInfo().getMainBorderColor())));
				} else {
					splitPane.setBorder(new JSplitPane().getBorder());
				}
			}
		}

		private PlanDiagram createDiagram() {
			PlanDiagram result = new PlanDiagram(swingRenderer, getPlan(), PlanEditor.this);
			result.addListener(new JDiagramListener() {

				@Override
				public void nodeMoved(JNode node) {
					Step step = (Step) node.getObject();
					ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
					ITypeInfo stepType = reflectionUI
							.getTypeInfo(new JavaTypeInfoSource(reflectionUI, Step.class, null));
					getModificationStack().insideComposite("Change Step Position", UndoOrder.getNormal(),
							new Accessor<Boolean>() {
								@Override
								public Boolean get() {
									ReflectionUIUtils
											.setFieldValueThroughModificationStack(
													new DefaultFieldControlData(reflectionUI, step,
															ReflectionUIUtils.findInfoByName(stepType.getFields(),
																	"diagramX")),
													node.getX(), getModificationStack(),
													ReflectionUIUtils.getDebugLogListener(reflectionUI));
									ReflectionUIUtils
											.setFieldValueThroughModificationStack(
													new DefaultFieldControlData(reflectionUI, step,
															ReflectionUIUtils.findInfoByName(stepType.getFields(),
																	"diagramY")),
													node.getY(), getModificationStack(),
													ReflectionUIUtils.getDebugLogListener(reflectionUI));
									return true;
								}
							}, false);
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
					ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
					ITypeInfo planType = reflectionUI
							.getTypeInfo(new JavaTypeInfoSource(reflectionUI, Plan.class, null));
					DefaultFieldControlData transitionsData = new DefaultFieldControlData(reflectionUI, getPlan(),
							ReflectionUIUtils.findInfoByName(planType.getFields(), "transitions"));
					IModification modification = new ListModificationFactory(
							new ItemPositionFactory(transitionsData).getRootItemPosition(-1)).add(0, newTransition);
					getModificationStack().apply(modification);
				}
			});
			result.setBackground(Color.WHITE);
			return result;
		}

	}

	public static class PlanExecutorView extends CustomizingForm {

		private static final long serialVersionUID = 1L;
		private PlanDiagram diagram;
		private boolean selectionListeningEnabled = true;
		private ControlSplitPane splitPane;

		public PlanExecutorView(SwingCustomizer swingRenderer, PlanExecutor planExecutor, IInfoFilter infoFilter) {
			super(swingRenderer, planExecutor, infoFilter);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					updateDiagram();
				}
			});
		}

		protected void updateDiagram() {
			if (diagram == null) {
				return;
			}
			diagram.refresh();
			ListControl stepOccurrencesControl = getStepOccurrencesControl();
			BufferedItemPosition selection = stepOccurrencesControl.getSingleSelection();
			if (selection != null) {
				diagram.select(diagram.getNode(((StepOccurrence) selection.getItem()).getStep()));
			} else {
				diagram.select(null);
			}
		}

		public PlanExecutor getPlanExecutor() {
			return (PlanExecutor) object;
		}

		@Override
		protected void createMembersControls() {
			super.createMembersControls();
			diagram = createDiagram();
			getStepOccurrencesControl().addListControlSelectionListener(new Listener<List<BufferedItemPosition>>() {
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

		private ListControl getStepOccurrencesControl() {
			return (ListControl) getFieldControlPlaceHolder("stepOccurrences").getFieldControl();
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

		private PlanDiagram createDiagram() {
			final PlanDiagram result = new PlanDiagram(swingRenderer, getPlanExecutor().getPlan(), null) {

				private static final long serialVersionUID = 1L;

				@Override
				protected JDiagramActionScheme createActionScheme() {
					return null;
				}

				@Override
				protected void paintNode(Graphics g, JNode node) {
					StepOccurrence currentStepOccurrence = getPlanExecutor().getCurrentStepOccurrence();
					if (currentStepOccurrence != null) {
						if (currentStepOccurrence.getStep() == node.getObject()) {
							highlightNode(g, node,
									(currentStepOccurrence.getActivityError() == null) ? new Color(175, 255, 200)
											: new Color(255, 173, 173));
						}
					}
					super.paintNode(g, node);
				}

				@Override
				protected void paintConnection(Graphics g, JConnection conn) {
					super.paintConnection(g, conn);
					int transitionOccurrenceCount = 0;
					List<StepOccurrence> stepOccurrences = getPlanExecutor().getStepOccurrences();
					for (int i = 0; i < stepOccurrences.size(); i++) {
						if (i > 0) {
							if (stepOccurrences.get(i - 1).getStep() == conn.getStartNode().getObject()) {
								if (stepOccurrences.get(i).getStep() == conn.getEndNode().getObject()) {
									transitionOccurrenceCount++;
								}
							}
						}
					}
					if (transitionOccurrenceCount > 0) {
						annotateConnection(g, conn, "(" + transitionOccurrenceCount + ")");
					}
				}

				void annotateConnection(Graphics g, JConnection conn, String annotation) {
					g.setColor(Color.BLUE);
					int x = (conn.getStartNode().getX() + conn.getEndNode().getX()) / 2;
					int y = (conn.getStartNode().getY() + conn.getEndNode().getY()) / 2;
					g.drawString(annotation, x, y);
				}

				void highlightNode(Graphics g, JNode node, Color color) {
					g.setColor(color);
					int width = (node.getImage().getWidth(null) * 3) / 2;
					int height = (node.getImage().getHeight(null) * 3) / 2;
					g.fillRoundRect(node.getX() - (width / 2), node.getY() - (height / 2), width, height, width / 10,
							height / 10);
				}

			};
			result.addListener(new JDiagramListener() {

				@Override
				public void nodeMoved(JNode node) {
					updateDiagram();
				}

				@Override
				public void nodeSelected(JNode node) {
					if (selectionListeningEnabled) {
						selectionListeningEnabled = false;
						try {
							if (node == null) {
								getStepOccurrencesControl().setSingleSelection(null);
							} else {
								Step step = (Step) node.getObject();
								StepOccurrence lastStepOccurrence = null;
								for (int i = getPlanExecutor().getStepOccurrences().size() - 1; i >= 0; i--) {
									StepOccurrence stepOccurrence = getPlanExecutor().getStepOccurrences().get(i);
									if (stepOccurrence.getStep() == step) {
										lastStepOccurrence = stepOccurrence;
										break;
									}
								}
								ListControl stepOccurrencesControl = getStepOccurrencesControl();
								stepOccurrencesControl
										.setSingleSelection(stepOccurrencesControl.getRootListItemPosition(
												getPlanExecutor().getStepOccurrences().indexOf(lastStepOccurrence)));
							}
						} finally {
							selectionListeningEnabled = true;
						}
					}
				}

				@Override
				public void connectionAdded(JConnection conn) {
					result.refresh();
				}
			});
			result.setBackground(Color.WHITE);
			return result;
		}

	}

	public static class PlanDiagram extends JDiagram {

		private static final long serialVersionUID = 1L;

		private SwingRenderer swingRenderer;
		private Plan plan;
		private PlanEditor planEditor;

		public PlanDiagram(SwingRenderer swingRenderer, Plan plan, PlanEditor planEditor) {
			this.swingRenderer = swingRenderer;
			this.plan = plan;
			this.planEditor = planEditor;
			setActionScheme(createActionScheme());
		}

		protected JDiagramActionScheme createActionScheme() {
			return new JDiagramActionScheme() {

				@Override
				public String getTitle() {
					return "Add";
				}

				@Override
				public List<JDiagramActionCategory> getActionCategories() {
					List<String> activityCategoryNames = new ArrayList<String>();
					for (ActivityMetadata metadata : JESBReflectionUI.ACTIVITY_METADATAS) {
						if (!activityCategoryNames.contains(metadata.getCategoryName())) {
							activityCategoryNames.add(metadata.getCategoryName());
						}
					}
					List<JDiagramActionCategory> result = new ArrayList<JDiagramActionCategory>();
					for (String name : activityCategoryNames) {
						result.add(new JDiagramActionCategory() {

							@Override
							public String getName() {
								return name;
							}

							@Override
							public List<JDiagramAction> getActions() {
								List<JDiagramAction> result = new ArrayList<JDiagramAction>();
								for (ActivityMetadata metadata : JESBReflectionUI.ACTIVITY_METADATAS) {
									if (name.equals(metadata.getCategoryName())) {
										result.add(new JDiagramAction() {

											@Override
											public void perform(int x, int y) {
												Step newStep = new Step(metadata);
												newStep.setDiagramX(x);
												newStep.setDiagramY(y);
												ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
												ITypeInfo planType = reflectionUI.getTypeInfo(
														new JavaTypeInfoSource(reflectionUI, Plan.class, null));
												DefaultFieldControlData transitionsData = new DefaultFieldControlData(
														reflectionUI, getPlan(), ReflectionUIUtils
																.findInfoByName(planType.getFields(), "steps"));
												IModification modification = new ListModificationFactory(
														new ItemPositionFactory(transitionsData)
																.getRootItemPosition(-1)).add(plan.getSteps().size(),
																		newStep);
												planEditor.getModificationStack().apply(modification);
												refresh();
											}

											@Override
											public String getLabel() {
												return metadata.getActivityTypeName();
											}

											@Override
											public Icon getIcon() {
												return SwingRendererUtils
														.getIcon(SwingRendererUtils.scalePreservingRatio(
																SwingRendererUtils.loadImageThroughCache(
																		metadata.getActivityIconImagePath(),
																		ReflectionUIUtils.getDebugLogListener(
																				swingRenderer.getReflectionUI())),
																32, 32, Image.SCALE_SMOOTH));
											}
										});
									}
								}
								return result;
							}
						});
					}
					return result;
				}
			};
		}

		public Plan getPlan() {
			return plan;
		}

		public void setPlan(Plan plan) {
			this.plan = plan;
		}

		public void refresh() {
			clear();
			for (Step step : plan.getSteps()) {
				JNode node = addNode(step, step.getDiagramX(), step.getDiagramY());
				ResourcePath iconImagePath = MiscUtils.getIconImagePath(step);
				if (iconImagePath != null) {
					node.setImage(SwingRendererUtils.loadImageThroughCache(iconImagePath,
							ReflectionUIUtils.getDebugLogListener(swingRenderer.getReflectionUI())));
				}
			}
			for (Transition t : plan.getTransitions()) {
				JNode node1 = getNode(t.getStartStep());
				JNode node2 = getNode(t.getEndStep());
				if ((node1 != null) && (node2 != null)) {
					addConnection(node1, node2);
				}
			}
			repaint();
		}

	}
}
