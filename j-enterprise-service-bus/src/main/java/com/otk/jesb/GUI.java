package com.otk.jesb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellRenderer;

import com.otk.jesb.Debugger.PlanActivator;
import com.otk.jesb.Debugger.PlanExecutor;
import com.otk.jesb.Structure.Element;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.activity.builtin.CallSOAPWebServiceActivity;
import com.otk.jesb.activity.builtin.ExecutePlanActivity;
import com.otk.jesb.activity.builtin.JDBCQueryActivity;
import com.otk.jesb.activity.builtin.JDBCUpdateActivity;
import com.otk.jesb.activity.builtin.ReadFileActivity;
import com.otk.jesb.activity.builtin.SleepActivity;
import com.otk.jesb.activity.builtin.WriteFileActivity;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.diagram.DragIntent;
import com.otk.jesb.diagram.JConnection;
import com.otk.jesb.diagram.JDiagram;
import com.otk.jesb.diagram.JDiagramAction;
import com.otk.jesb.diagram.JDiagramActionCategory;
import com.otk.jesb.diagram.JDiagramActionScheme;
import com.otk.jesb.diagram.JDiagramListener;
import com.otk.jesb.diagram.JNode;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FieldInitializer;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.instantiation.InitializationCase;
import com.otk.jesb.instantiation.InitializationCaseFacade;
import com.otk.jesb.instantiation.InitializationSwitchFacade;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.instantiation.ValueMode;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.resource.builtin.WSDL;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.SquigglePainter;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.control.DefaultFieldControlData;
import xy.reflect.ui.control.IAdvancedFieldControl;
import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.NullableControl;
import xy.reflect.ui.control.swing.TextControl;
import xy.reflect.ui.control.swing.customizer.CustomizingFieldControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.customizer.CustomizingMethodControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;
import xy.reflect.ui.control.swing.renderer.FieldControlPlaceHolder;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.ControlPanel;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.field.CapsuleFieldInfo;
import xy.reflect.ui.info.field.FieldInfoProxy;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.filter.IInfoFilter;
import xy.reflect.ui.info.menu.MenuModel;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.parameter.ParameterInfoProxy;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationItemInfo;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.info.type.iterable.item.ItemPosition;
import xy.reflect.ui.info.type.iterable.item.ItemPositionFactory;
import xy.reflect.ui.info.type.iterable.util.AbstractDynamicListAction;
import xy.reflect.ui.info.type.iterable.util.DynamicListActionProxy;
import xy.reflect.ui.info.type.iterable.util.IDynamicListAction;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.info.type.source.SpecificitiesIdentifier;
import xy.reflect.ui.undo.IModification;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.undo.UndoOrder;
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
		((InstanceBuilder) ((ParameterInitializer) ab2.getInstanceBuilder().getRootInitializer()).getParameterValue())
				.getFieldInitializers().add(new FieldInitializer("filePath", "tmp/test.txt"));
		((InstanceBuilder) ((ParameterInitializer) ab2.getInstanceBuilder().getRootInitializer()).getParameterValue())
				.getFieldInitializers().add(new FieldInitializer("text",
						new Function("return a.getRows().get(0).getCellValues().get(\"TABLE_NAME\");")));

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
		return new CustomizingForm(this, object, infoFilter) {

			private static final long serialVersionUID = 1L;

			@Override
			protected CustomizingFieldControlPlaceHolder createFieldControlPlaceHolder(IFieldInfo field) {
				final CustomizingForm thisForm = this;
				return new CustomizingFieldControlPlaceHolder(this, field) {

					private static final long serialVersionUID = 1L;

					@Override
					public Component createFieldControl() {
						if (field.getType().getName().equals(PlanDiagram.Source.class.getName())) {
							return new PlanDiagram(swingRenderer, thisForm);
						}
						if (field.getType().getName().equals(PlanDiagram.PaletteSource.class.getName())) {
							return new PlanDiagramPalette(swingRenderer, thisForm);
						}
						if (field.getType().getName().equals(DebugPlanDiagram.Source.class.getName())) {
							return new DebugPlanDiagram(swingRenderer, thisForm);
						}
						if (object instanceof RootInstanceBuilderFacade) {
							if (field.getName().equals("children")) {
								return new InstanceBuilderInitializerTreeControl(GUI.this, this);
							}
						}
						if (object instanceof RootInstanceBuilder) {
							if (field.getName().equals("data")) {
								return new InstanceBuilderVariableTreeControl(GUI.this, this);
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
													return facade.createDefaultFieldValue();
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
													return facade.createDefaultParameterValue();
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
													return facade.createDefaultItemValue();
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
					public void refreshUI(boolean refreshStructure) {
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
							if (field.getName().equals("typeGroup")) {
								if (((InstanceBuilderFacade) object).getUnderlying()
										.getDynamicTypeNameAccessor() != null) {
									setVisible(false);
								}
							}
						}
						super.refreshUI(refreshStructure);
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
				final CustomizingForm thisForm = this;
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
						((FunctionEditor) object).validate();
					} catch (CompilationError e) {
						if (textComponent.getText() != null) {
							textComponent.getHighlighter().addHighlight(
									(e.getStartPosition() == -1) ? 0 : e.getStartPosition(),
									(e.getEndPosition() == -1) ? textComponent.getText().length() : e.getEndPosition(),
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

	public static class InstanceBuilderInitializerTreeControl extends ListControl {

		private static final long serialVersionUID = 1L;

		public InstanceBuilderInitializerTreeControl(SwingRenderer swingRenderer, IFieldControlInput input) {
			super(swingRenderer, input);
			expandItemPositions(2);
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

	public static class InstanceBuilderVariableTreeControl extends ListControl {

		private static final long serialVersionUID = 1L;

		public InstanceBuilderVariableTreeControl(SwingRenderer swingRenderer, IFieldControlInput input) {
			super(swingRenderer, input);
		}

	}

	public static class JESBReflectionUI extends CustomizedUI {

		public static final List<ActivityMetadata> ACTIVITY_METADATAS = Arrays.asList(new SleepActivity.Metadata(),
				new ExecutePlanActivity.Metadata(), new ReadFileActivity.Metadata(), new WriteFileActivity.Metadata(),
				new JDBCQueryActivity.Metadata(), new JDBCUpdateActivity.Metadata(),
				new CallSOAPWebServiceActivity.Metadata());
		public static final List<ResourceMetadata> RESOURCE_METADATAS = Arrays.asList(new JDBCConnection.Metadata(),
				new WSDL.Metadata());

		private static WeakHashMap<RootInstanceBuilder, ByteArrayOutputStream> rootInitializerStoreByBuilder = new WeakHashMap<RootInstanceBuilder, ByteArrayOutputStream>();
		private static WeakHashMap<Plan, DragIntent> diagramDragIntentByPlan = new WeakHashMap<Plan, DragIntent>();

		private Plan currentPlan;
		private Step currentStep;

		@Override
		protected ITypeInfo getTypeInfoBeforeCustomizations(ITypeInfo type) {
			return new InfoProxyFactory() {

				void backupRootInstanceBuilderState(RootInstanceBuilder rootInstanceBuilder) {
					Object rootInitializer = rootInstanceBuilder.getRootInitializer();
					ByteArrayOutputStream rootInitializerStore;
					if (rootInitializer != null) {
						rootInitializerStore = new ByteArrayOutputStream();
						try {
							MiscUtils.serialize(rootInitializer, rootInitializerStore);
						} catch (IOException e) {
							throw new AssertionError(e);
						}
					} else {
						rootInitializerStore = null;
					}
					rootInitializerStoreByBuilder.put(rootInstanceBuilder, rootInitializerStore);
				}

				Runnable createRootInstanceBuilderStateRestorationJob(RootInstanceBuilder rootInstanceBuilder) {
					if (!rootInitializerStoreByBuilder.containsKey(rootInstanceBuilder)) {
						throw new AssertionError();
					}
					final ByteArrayOutputStream rootInitializerStore = rootInitializerStoreByBuilder
							.get(rootInstanceBuilder);
					return new Runnable() {
						@Override
						public void run() {
							Object rootInitializerCopy;
							try {
								rootInitializerCopy = (rootInitializerStore == null) ? null
										: MiscUtils.deserialize(
												new ByteArrayInputStream(rootInitializerStore.toByteArray()));
							} catch (IOException e) {
								throw new AssertionError(e);
							}
							rootInstanceBuilder.setRootInitializer(
									(rootInitializerCopy == null) ? null : MiscUtils.copy(rootInitializerCopy));
						}
					};
				}

				@Override
				protected void onFormRefresh(ITypeInfo type, Object object) {
					if (object instanceof RootInstanceBuilderFacade) {
						backupRootInstanceBuilderState(((RootInstanceBuilderFacade) object).getUnderlying());
					}
					super.onFormRefresh(type, object);
				}

				@Override
				protected Runnable getLastFormRefreshStateRestorationJob(ITypeInfo type, Object object) {
					if (object instanceof RootInstanceBuilderFacade) {
						return createRootInstanceBuilderStateRestorationJob(
								((RootInstanceBuilderFacade) object).getUnderlying());
					}
					return super.getLastFormRefreshStateRestorationJob(type, object);
				}

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
				protected List<IMethodInfo> getAlternativeListItemConstructors(IFieldInfo field, Object object,
						ITypeInfo objectType) {
					if (objectType.getName().equals(InitializationSwitchFacade.class.getName())) {
						if (field.getName().equals("children")) {
							return Collections.singletonList(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

								@Override
								public String getSignature() {
									return ReflectionUIUtils.buildMethodSignature(this);
								}

								@Override
								public String getName() {
									return "";
								}

								@Override
								public Object invoke(Object object, InvocationData invocationData) {
									InitializationSwitchFacade switchFacade = (InitializationSwitchFacade) object;
									Function condition = InitializationCase.createDefaultCondition();
									InitializationCase initializationCase = new InitializationCase();
									return new InitializationCaseFacade(switchFacade, condition, initializationCase);
								}

							});
						}
					}
					return super.getAlternativeListItemConstructors(field, object, objectType);
				}

				@Override
				protected List<IDynamicListAction> getDynamicActions(IListTypeInfo type,
						List<? extends ItemPosition> selection,
						Mapper<ItemPosition, ListModificationFactory> listModificationFactoryAccessor) {
					List<IDynamicListAction> result = new ArrayList<IDynamicListAction>(
							super.getDynamicActions(type, selection, listModificationFactoryAccessor));
					if (selection.size() > 0) {
						final ItemPosition firstItemPosition = selection.get(0);
						if (selection.stream().allMatch(
								itemPosition -> ((itemPosition.getItem() instanceof ParameterInitializerFacade)
										|| (itemPosition.getItem() instanceof FieldInitializerFacade)
										|| (itemPosition.getItem() instanceof ListItemInitializerFacade)
										|| (itemPosition.getItem() instanceof InitializationSwitchFacade))
										&& MiscUtils.equalsOrBothNull(itemPosition.getParentItemPosition(),
												firstItemPosition.getParentItemPosition()))) {
							final Facade parentFacade = ((Facade) firstItemPosition.getItem()).getParent();
							result.add(new AbstractDynamicListAction() {

								@Override
								public String getName() {
									return "insertSwitchCasesParent";
								}

								@Override
								public String getCaption() {
									return "Insert Switch/Cases Parent...";
								}

								@Override
								public String getParametersValidationCustomCaption() {
									return "OK";
								}

								@Override
								public DisplayMode getDisplayMode() {
									return DisplayMode.CONTEXT_MENU;
								}

								@Override
								public List<IParameterInfo> getParameters() {
									return Collections
											.singletonList(new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

												@Override
												public String getName() {
													return "caseCount";
												}

												@Override
												public String getCaption() {
													return "Number Of Cases";
												}

												@Override
												public ITypeInfo getType() {
													return getTypeInfo(new JavaTypeInfoSource(int.class, null));
												}

												@Override
												public Object getDefaultValue(Object object) {
													return 2;
												}

												@Override
												public int getPosition() {
													return 0;
												}

											});
								}

								@Override
								public Object invoke(Object object, InvocationData invocationData) {
									List<Facade> initializerFacades = selection.stream()
											.map(itemPosition -> (Facade) itemPosition.getItem())
											.collect(Collectors.toList());
									int caseCount = (int) invocationData.getParameterValue(0);
									InitializationSwitchFacade.install(parentFacade, caseCount, initializerFacades);
									return null;
								}

								@Override
								public List<ItemPosition> getPostSelection() {
									return Collections.singletonList(
											firstItemPosition.getSubItemPosition(0).getSubItemPosition(0));
								}

							});
							List<? extends Facade> siblingSwitchFacades = parentFacade.getChildren().stream().filter(
									facade -> (facade instanceof InitializationSwitchFacade) && !selection.stream()
											.anyMatch(itemPosition -> ((Facade) itemPosition.getItem())
													.getUnderlying() == facade.getUnderlying()))
									.collect(Collectors.toList());
							if (siblingSwitchFacades.size() > 0) {
								result.add(new AbstractDynamicListAction() {

									@Override
									public String getName() {
										return "moveIntoSiblingSwitchCases";
									}

									@Override
									public String getCaption() {
										return "Move Into Sibling Switch/Cases...";
									}

									@Override
									public String getParametersValidationCustomCaption() {
										return "OK";
									}

									@Override
									public DisplayMode getDisplayMode() {
										return DisplayMode.CONTEXT_MENU;
									}

									@Override
									public List<IParameterInfo> getParameters() {
										return Collections.singletonList(
												new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

													@Override
													public String getName() {
														return "siblingSwitchFacade";
													}

													@Override
													public String getCaption() {
														return "Sibling Swith/Cases";
													}

													@Override
													public ITypeInfo getType() {
														return getTypeInfo(new JavaTypeInfoSource(
																InitializationSwitchFacade.class, null));
													}

													@Override
													public Object getDefaultValue(Object object) {
														return siblingSwitchFacades.get(0);
													}

													@Override
													public int getPosition() {
														return 0;
													}

													@Override
													public boolean hasValueOptions(Object object) {
														return true;
													}

													@Override
													public Object[] getValueOptions(Object object) {
														return siblingSwitchFacades.toArray();
													}

												});
									}

									@Override
									public Object invoke(Object object, InvocationData invocationData) {
										List<Facade> initializerFacades = selection.stream()
												.map(itemPosition -> (Facade) itemPosition.getItem())
												.collect(Collectors.toList());
										InitializationSwitchFacade selectedSwitchFacade = (InitializationSwitchFacade) invocationData
												.getParameterValue(0);
										selectedSwitchFacade.importInitializerFacades(initializerFacades);
										return null;
									}
								});
							}
						}
					}
					return result;
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
				protected List<IFieldInfo> getFields(ITypeInfo type) {
					if (type.getName().equals(Plan.class.getName())) {
						List<IFieldInfo> result = new ArrayList<IFieldInfo>(super.getFields(type));
						result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {
							@Override
							public String getName() {
								return "diagram";
							}

							@Override
							public String getCaption() {
								return "Diagram";
							}

							@Override
							public Object getValue(Object object) {
								return new PlanDiagram.Source();
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(PlanDiagram.Source.class, null));
							}

						});
						result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {
							@Override
							public String getName() {
								return "palette";
							}

							@Override
							public String getCaption() {
								return "Palette";
							}

							@Override
							public Object getValue(Object object) {
								return new PlanDiagram.PaletteSource();
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(PlanDiagram.PaletteSource.class, null));
							}

						});
						result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {
							@Override
							public String getName() {
								return "diagramDragIntent";
							}

							@Override
							public String getCaption() {
								return "Diagram Drag Intent";
							}

							@Override
							public boolean isGetOnly() {
								return false;
							}

							@Override
							public boolean isTransient() {
								return true;
							}

							@Override
							public Object getValue(Object object) {
								return (DragIntent) diagramDragIntentByPlan.getOrDefault((Plan) object,
										DragIntent.MOVE);
							}

							@Override
							public void setValue(Object object, Object value) {
								diagramDragIntentByPlan.put((Plan) object, (DragIntent) value);
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(DragIntent.class,
										new SpecificitiesIdentifier(Plan.class.getName(), getName())));
							}

						});
						return result;
					} else if (type.getName().equals(PlanExecutor.class.getName())) {
						List<IFieldInfo> result = new ArrayList<IFieldInfo>(super.getFields(type));
						result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {
							@Override
							public String getName() {
								return "diagram";
							}

							@Override
							public String getCaption() {
								return "Diagram";
							}

							@Override
							public Object getValue(Object object) {
								return new DebugPlanDiagram.Source();
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(DebugPlanDiagram.Source.class, null));
							}

						});
						return result;
					} else if (type.getName().equals(RootInstanceBuilder.class.getName())) {
						List<IFieldInfo> result = new ArrayList<IFieldInfo>(super.getFields(type));
						result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {
							@Override
							public String getName() {
								return "data";
							}

							@Override
							public String getCaption() {
								return "Data";
							}

							@Override
							public Object getValue(Object object) {
								return new PathOptionsProvider(currentPlan, currentStep).getRootPathNodes();
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(List.class,
										new SpecificitiesIdentifier(RootInstanceBuilder.class.getName(), getName())));
							}

						});
						return result;
					} else {
						return super.getFields(type);
					}
				}

				@Override
				protected List<IMethodInfo> getMethods(ITypeInfo type) {
					if (type.getName().equals(Function.class.getName())) {
						List<IMethodInfo> result = new ArrayList<IMethodInfo>(super.getMethods(type));
						result.add(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

							@Override
							public String getSignature() {
								return ReflectionUIUtils.buildMethodSignature(this);
							}

							@Override
							public String getName() {
								return "assist";
							}

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
								return getTypeInfo(new JavaTypeInfoSource(FunctionEditor.class, null));
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
							result.add(getTypeInfo(
									new JavaTypeInfoSource(activityMetadata.getActivityBuilderClass(), null)));
						}
						return result;
					} else if (type.getName().equals(Resource.class.getName())) {
						List<ITypeInfo> result = new ArrayList<ITypeInfo>();
						for (ResourceMetadata resourceMetadata : RESOURCE_METADATAS) {
							result.add(getTypeInfo(new JavaTypeInfoSource(resourceMetadata.getResourceClass(), null)));
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
							return getIconImagePath(
									getTypeInfo(new JavaTypeInfoSource(ParameterInitializerFacade.class, null)), null);
						} else {
							return getIconImagePath(
									getTypeInfo(new JavaTypeInfoSource(FieldInitializerFacade.class, null)), null);
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

	public static class DebugPlanDiagram extends PlanDiagram {

		private static final long serialVersionUID = 1L;

		public DebugPlanDiagram(SwingRenderer swingRenderer, CustomizingForm parentForm) {
			super(swingRenderer, parentForm);
		}

		protected ListControl getStepOccurrencesControl() {
			List<Form> forms = new ArrayList<Form>();
			forms.add(getPlanExecutorView());
			forms.addAll(SwingRendererUtils.findDescendantForms(getPlanExecutorView(), swingRenderer));
			for (Form form : forms) {
				FieldControlPlaceHolder stepOccurrencesFieldControlPlaceHolder = form
						.getFieldControlPlaceHolder("stepOccurrences");
				if (stepOccurrencesFieldControlPlaceHolder != null) {
					return (ListControl) stepOccurrencesFieldControlPlaceHolder.getFieldControl();
				}
			}
			throw new AssertionError();
		}

		protected Form getPlanExecutorView() {
			if (parentForm.getObject() instanceof PlanExecutor) {
				return parentForm;
			}
			return SwingRendererUtils.findAncestorFormOfType(parentForm, PlanExecutor.class.getName(), swingRenderer);
		}

		@Override
		protected void updateExternalComponentsOnInternalEvents() {
			addListener(new JDiagramListener() {

				@Override
				public void nodeMoved(JNode node) {
					refreshUI(false);
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
					refreshUI(false);
				}
			});
		}

		@Override
		protected void updateInternalComponentsOnExternalEvents() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					getStepOccurrencesControl()
							.addListControlSelectionListener(new Listener<List<BufferedItemPosition>>() {
								@Override
								public void handle(List<BufferedItemPosition> event) {
									if (selectionListeningEnabled) {
										selectionListeningEnabled = false;
										try {
											updateStepSelection();
										} finally {
											selectionListeningEnabled = true;
										}
									}
								}
							});
				}
			});
		}

		@Override
		protected void updateStepSelection() {
			ListControl stepOccurrencesControl = getStepOccurrencesControl();
			BufferedItemPosition selection = stepOccurrencesControl.getSingleSelection();
			if (selection != null) {
				select(getNode(((StepOccurrence) selection.getItem()).getStep()));
			} else {
				select(null);
			}
		}

		@Override
		protected JDiagramActionScheme createActionScheme() {
			return null;
		}

		public PlanExecutor getPlanExecutor() {
			return (PlanExecutor) getPlanExecutorView().getObject();
		}

		@Override
		public Plan getPlan() {
			return getPlanExecutor().getPlan();
		}

		@Override
		protected void paintNode(Graphics g, JNode node) {
			StepOccurrence currentStepOccurrence = getPlanExecutor().getCurrentStepOccurrence();
			if (currentStepOccurrence != null) {
				if (currentStepOccurrence.getStep() == node.getObject()) {
					highlightNode(g, node, (currentStepOccurrence.getActivityError() == null) ? new Color(175, 255, 200)
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
			int x = (conn.getStartNode().getCenterX() + conn.getEndNode().getCenterX()) / 2;
			int y = (conn.getStartNode().getCenterY() + conn.getEndNode().getCenterY()) / 2;
			g.drawString(annotation, x, y);
		}

		void highlightNode(Graphics g, JNode node, Color color) {
			g.setColor(color);
			int width = (node.getImage().getWidth(null) * 3) / 2;
			int height = (node.getImage().getHeight(null) * 3) / 2;
			g.fillRoundRect(node.getCenterX() - (width / 2), node.getCenterY() - (height / 2), width, height,
					width / 10, height / 10);
		}

		public static class Source {
		}
	}

	public static class PlanDiagramPalette extends ControlPanel implements IAdvancedFieldControl {

		private static final long serialVersionUID = 1L;

		private CustomizingForm parentForm;
		private SwingRenderer swingRenderer;

		public PlanDiagramPalette(SwingRenderer swingRenderer, CustomizingForm parentForm) {
			this.swingRenderer = swingRenderer;
			this.parentForm = parentForm;
			setLayout(new BorderLayout());
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					PlanDiagramPalette.this.refreshUI(true);
				}
			});
		}

		protected PlanDiagram getDiagram() {
			List<Form> forms = new ArrayList<Form>();
			forms.add(getPlanEditor());
			forms.addAll(SwingRendererUtils.findDescendantForms(getPlanEditor(), swingRenderer));
			for (Form form : forms) {
				FieldControlPlaceHolder diagramFieldControlPlaceHolder = form.getFieldControlPlaceHolder("diagram");
				if (diagramFieldControlPlaceHolder != null) {
					if (diagramFieldControlPlaceHolder.getFieldControl() instanceof PlanDiagram) {
						return (PlanDiagram) diagramFieldControlPlaceHolder.getFieldControl();
					}
				}
			}
			return null;
		}

		protected Form getPlanEditor() {
			if (parentForm.getObject() instanceof Plan) {
				return parentForm;
			}
			return SwingRendererUtils.findAncestorFormOfType(parentForm, Plan.class.getName(), swingRenderer);
		}

		@Override
		public boolean showsCaption() {
			return false;
		}

		@Override
		public boolean refreshUI(boolean refreshStructure) {
			if (refreshStructure) {
				removeAll();
				if (getDiagram() != null) {
					add(BorderLayout.CENTER, getDiagram().createActionPalette(JTabbedPane.RIGHT, BoxLayout.Y_AXIS));
					SwingRendererUtils.handleComponentSizeChange(PlanDiagramPalette.this);
				}
			}
			return true;
		}

		@Override
		public void validateSubForms() throws Exception {
		}

		@Override
		public void addMenuContributions(MenuModel menuModel) {
		}

		@Override
		public boolean requestCustomFocus() {
			return false;
		}

		@Override
		public boolean isAutoManaged() {
			return false;
		}

		@Override
		public boolean displayError(String msg) {
			return false;
		}
	}

	public static class PlanDiagram extends JDiagram implements IAdvancedFieldControl {

		private static final long serialVersionUID = 1L;

		protected SwingRenderer swingRenderer;
		protected CustomizingForm parentForm;
		protected boolean selectionListeningEnabled = true;

		public PlanDiagram(SwingRenderer swingRenderer, CustomizingForm parentForm) {
			this.swingRenderer = swingRenderer;
			this.parentForm = parentForm;
			setActionScheme(createActionScheme());
			updateExternalComponentsOnInternalEvents();
			updateInternalComponentsOnExternalEvents();
			setBackground(Color.WHITE);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					selectionListeningEnabled = false;
					try {
						PlanDiagram.this.refreshUI(true);
						updateStepSelection();
					} finally {
						selectionListeningEnabled = true;
					}
				}
			});
		}

		public Plan getPlan() {
			return (Plan) getPlanEditor().getObject();
		}

		protected ListControl getStepsControl() {
			List<Form> forms = new ArrayList<Form>();
			forms.add(getPlanEditor());
			forms.addAll(SwingRendererUtils.findDescendantForms(getPlanEditor(), swingRenderer));
			for (Form form : forms) {
				FieldControlPlaceHolder stepsFieldControlPlaceHolder = form.getFieldControlPlaceHolder("steps");
				if (stepsFieldControlPlaceHolder != null) {
					return (ListControl) stepsFieldControlPlaceHolder.getFieldControl();
				}
			}
			throw new AssertionError();
		}

		protected Form getPlanEditor() {
			if (parentForm.getObject() instanceof Plan) {
				return parentForm;
			}
			return SwingRendererUtils.findAncestorFormOfType(parentForm, Plan.class.getName(), swingRenderer);
		}

		protected void updateExternalComponentsOnInternalEvents() {
			addListener(new JDiagramListener() {

				@Override
				public void nodeMoved(JNode node) {
					Step step = (Step) node.getObject();
					ReflectionUI reflectionUI = swingRenderer.getReflectionUI();
					ITypeInfo stepType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Step.class, null));
					parentForm.getModificationStack().insideComposite("Change Step Position", UndoOrder.getNormal(),
							new xy.reflect.ui.util.Accessor<Boolean>() {
								@Override
								public Boolean get() {
									ReflectionUIUtils
											.setFieldValueThroughModificationStack(
													new DefaultFieldControlData(reflectionUI, step,
															ReflectionUIUtils.findInfoByName(stepType.getFields(),
																	"diagramX")),
													node.getCenterX(), parentForm.getModificationStack(),
													ReflectionUIUtils.getDebugLogListener(reflectionUI));
									ReflectionUIUtils
											.setFieldValueThroughModificationStack(
													new DefaultFieldControlData(reflectionUI, step,
															ReflectionUIUtils.findInfoByName(stepType.getFields(),
																	"diagramY")),
													node.getCenterY(), parentForm.getModificationStack(),
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
					ITypeInfo planType = reflectionUI.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
					DefaultFieldControlData transitionsData = new DefaultFieldControlData(reflectionUI, getPlan(),
							ReflectionUIUtils.findInfoByName(planType.getFields(), "transitions"));
					IModification modification = new ListModificationFactory(
							new ItemPositionFactory(transitionsData).getRootItemPosition(-1)).add(0, newTransition);
					parentForm.getModificationStack().apply(modification);
				}
			});
		}

		protected void updateInternalComponentsOnExternalEvents() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					getStepsControl().addListControlSelectionListener(new Listener<List<BufferedItemPosition>>() {
						@Override
						public void handle(List<BufferedItemPosition> event) {
							if (selectionListeningEnabled) {
								selectionListeningEnabled = false;
								try {
									updateStepSelection();
								} finally {
									selectionListeningEnabled = true;
								}
							}
						}
					});
				}
			});
		}

		protected void updateStepSelection() {
			ListControl stepsControl = getStepsControl();
			BufferedItemPosition selection = stepsControl.getSingleSelection();
			if (selection != null) {
				select(getNode(selection.getItem()));
			} else {
				select(null);
			}
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
												ITypeInfo planType = reflectionUI
														.getTypeInfo(new JavaTypeInfoSource(Plan.class, null));
												DefaultFieldControlData transitionsData = new DefaultFieldControlData(
														reflectionUI, getPlan(), ReflectionUIUtils
																.findInfoByName(planType.getFields(), "steps"));
												IModification modification = new ListModificationFactory(
														new ItemPositionFactory(transitionsData)
																.getRootItemPosition(-1))
																		.add(getPlan().getSteps().size(), newStep);
												parentForm.getModificationStack().apply(modification);
												refreshUI(false);
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

		@Override
		public boolean showsCaption() {
			return false;
		}

		@Override
		public boolean refreshUI(boolean refreshStructure) {
			Plan plan = getPlan();
			setDragIntent(JESBReflectionUI.diagramDragIntentByPlan.getOrDefault(plan, DragIntent.MOVE));
			JNode selectedNode = getSelectedNode();
			Step selectedStep = (selectedNode != null) ? (Step) selectedNode.getObject() : null;
			clear();
			for (Step step : plan.getSteps()) {
				JNode node = addNode(step, step.getDiagramX(), step.getDiagramY());
				ResourcePath iconImagePath = MiscUtils.getIconImagePath(step);
				if (iconImagePath != null) {
					node.setImage(SwingRendererUtils.loadImageThroughCache(iconImagePath,
							ReflectionUIUtils.getDebugLogListener(swingRenderer.getReflectionUI())));
				}
			}
			select((selectedStep != null) ? getNode(selectedStep) : null);
			for (Transition t : plan.getTransitions()) {
				JNode node1 = getNode(t.getStartStep());
				JNode node2 = getNode(t.getEndStep());
				if ((node1 != null) && (node2 != null)) {
					addConnection(node1, node2);
				}
			}
			SwingRendererUtils.handleComponentSizeChange(this);
			return true;
		}

		@Override
		public void validateSubForms() throws Exception {
		}

		@Override
		public void addMenuContributions(MenuModel menuModel) {
		}

		@Override
		public boolean requestCustomFocus() {
			return false;
		}

		@Override
		public boolean isAutoManaged() {
			return false;
		}

		@Override
		public boolean displayError(String msg) {
			return false;
		}

		public static class Source {
		}

		public static class PaletteSource {
		}
	}
}
