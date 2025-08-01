package com.otk.jesb.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import com.otk.jesb.FunctionEditor;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.PathOptionsProvider;
import com.otk.jesb.Structure;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.Debugger;
import com.otk.jesb.Debugger.PlanActivator;
import com.otk.jesb.Debugger.PlanExecutor;
import com.otk.jesb.Function;
import com.otk.jesb.Structure.Element;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FieldInitializer;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.InitializationCase;
import com.otk.jesb.instantiation.InitializationCaseFacade;
import com.otk.jesb.instantiation.InitializationSwitch;
import com.otk.jesb.instantiation.InitializationSwitchFacade;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.ListItemInitializer;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.ListItemReplicationFacade;
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.operation.builtin.CallSOAPWebService;
import com.otk.jesb.operation.builtin.DoNothing;
import com.otk.jesb.operation.builtin.Evaluate;
import com.otk.jesb.operation.builtin.ExecutePlan;
import com.otk.jesb.operation.builtin.JDBCQuery;
import com.otk.jesb.operation.builtin.JDBCUpdate;
import com.otk.jesb.operation.builtin.ReadFile;
import com.otk.jesb.operation.builtin.Sleep;
import com.otk.jesb.operation.builtin.WriteFile;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.resource.builtin.SharedStructureModel;
import com.otk.jesb.resource.builtin.WSDL;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.solution.Transition;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.LoopCompositeStep;
import com.otk.jesb.solution.LoopCompositeStep.LoopOperation;
import com.otk.jesb.solution.LoopCompositeStep.LoopOperation.Builder.ResultsCollectionConfigurationEntry;
import com.otk.jesb.ui.diagram.DragIntent;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.ValidationSession;
import xy.reflect.ui.info.field.FieldInfoProxy;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.field.ValueAsListFieldInfo;
import xy.reflect.ui.info.filter.IInfoFilter;
import xy.reflect.ui.info.filter.InfoFilterProxy;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.parameter.ParameterInfoProxy;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.item.EmbeddedItemDetailsAccessMode;
import xy.reflect.ui.info.type.iterable.item.IListItemDetailsAccessMode;
import xy.reflect.ui.info.type.iterable.item.ItemPosition;
import xy.reflect.ui.info.type.iterable.structure.DefaultListStructuralInfo;
import xy.reflect.ui.info.type.iterable.structure.IListStructuralInfo;
import xy.reflect.ui.info.type.iterable.util.AbstractDynamicListAction;
import xy.reflect.ui.info.type.iterable.util.DynamicListActionProxy;
import xy.reflect.ui.info.type.iterable.util.IDynamicListAction;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.info.type.source.SpecificitiesIdentifier;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.Mapper;
import xy.reflect.ui.util.ReflectionUIError;
import xy.reflect.ui.util.ReflectionUIUtils;

public class JESBReflectionUI extends CustomizedUI {

	public static final List<OperationMetadata> OPERATION_METADATAS = Arrays.asList(new DoNothing.Metadata(),
			new Evaluate.Metadata(), new Sleep.Metadata(), new ExecutePlan.Metadata(), new ReadFile.Metadata(),
			new WriteFile.Metadata(), new JDBCQuery.Metadata(), new JDBCUpdate.Metadata(),
			new CallSOAPWebService.Metadata());
	public static final List<OperationMetadata> COMPOSITE_METADATAS = Arrays.asList(new LoopOperation.Metadata());
	public static final List<ResourceMetadata> RESOURCE_METADATAS = Arrays.asList(new SharedStructureModel.Metadata(),
			new JDBCConnection.Metadata(), new WSDL.Metadata());
	private static final String CURRENT_VALIDATION_PLAN_KEY = JESBReflectionUI.class.getName()
			+ ".CURRENT_VALIDATION_PLAN_KEY";
	private static final String CURRENT_VALIDATION_STEP_KEY = JESBReflectionUI.class.getName()
			+ ".CURRENT_VALIDATION_STEP_KEY";
	private static final String CURRENT_VALIDATION_TRANSITION_KEY = JESBReflectionUI.class.getName()
			+ ".CURRENT_VALIDATION_TRANSITION_KEY";

	private static WeakHashMap<RootInstanceBuilder, ByteArrayOutputStream> rootInitializerStoreByBuilder = new WeakHashMap<RootInstanceBuilder, ByteArrayOutputStream>();
	static WeakHashMap<Plan, DragIntent> diagramDragIntentByPlan = new WeakHashMap<Plan, DragIntent>();

	private Plan displayedPlan;
	private Step displayedStep;
	private Transition displayedTransition;
	private RootInstanceBuilderFacade displayedRootInstanceBuilderFacade;
	private String sidePaneValueName;

	public static void backupRootInstanceBuilderState(RootInstanceBuilder rootInstanceBuilder) {
		Object rootInitializer = rootInstanceBuilder.getRootInitializer();
		ByteArrayOutputStream rootInitializerStore;
		if (rootInitializer != null) {
			rootInitializerStore = new ByteArrayOutputStream();
			try {
				MiscUtils.serialize(rootInitializer, rootInitializerStore);
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
		} else {
			rootInitializerStore = null;
		}
		rootInitializerStoreByBuilder.put(rootInstanceBuilder, rootInitializerStore);
	}

	public static Runnable getRootInstanceBuilderStateRestorationJob(RootInstanceBuilder rootInstanceBuilder) {
		if (!rootInitializerStoreByBuilder.containsKey(rootInstanceBuilder)) {
			throw new UnexpectedError();
		}
		final ByteArrayOutputStream rootInitializerStore = rootInitializerStoreByBuilder.get(rootInstanceBuilder);
		return new Runnable() {
			@Override
			public void run() {
				Object rootInitializerCopy;
				try {
					rootInitializerCopy = (rootInitializerStore == null) ? null
							: MiscUtils.deserialize(new ByteArrayInputStream(rootInitializerStore.toByteArray()));
				} catch (IOException e) {
					throw new UnexpectedError(e);
				}
				rootInstanceBuilder
						.setRootInitializer((rootInitializerCopy == null) ? null : MiscUtils.copy(rootInitializerCopy));
			}
		};
	}

	@Override
	protected ITypeInfo getTypeInfoBeforeCustomizations(ITypeInfo type) {
		return new InfoProxyFactory() {

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
					return getRootInstanceBuilderStateRestorationJob(
							((RootInstanceBuilderFacade) object).getUnderlying());
				}
				return super.getLastFormRefreshStateRestorationJob(type, object);
			}

			@Override
			protected Runnable getNextInvocationUndoJob(IMethodInfo method, ITypeInfo objectType, final Object object,
					InvocationData invocationData) {
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
			protected List<IMethodInfo> getAlternativeConstructors(IFieldInfo field, Object object,
					ITypeInfo objectType) {
				if (object instanceof ListItemInitializerFacade) {
					if (field.getName().equals("itemReplicationFacade")) {
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
								return new ListItemReplicationFacade((ListItemInitializerFacade) object);
							}

						});
					}
				}
				return super.getAlternativeConstructors(field, object, objectType);
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
								InstantiationFunction condition = InitializationCase.createDefaultCondition();
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
					if (selection.stream()
							.allMatch(itemPosition -> ((itemPosition.getItem() instanceof ParameterInitializerFacade)
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
								return Collections
										.singletonList(firstItemPosition.getSubItemPosition(0).getSubItemPosition(0));
							}

						});
						List<? extends Facade> siblingSwitchFacades = parentFacade.getChildren().stream()
								.filter(facade -> (facade instanceof InitializationSwitchFacade) && !selection.stream()
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
									return Collections
											.singletonList(new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

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
						if (parentFacade instanceof InitializationCaseFacade) {
							final ItemPosition parentCasePosition = firstItemPosition.getParentItemPosition();
							final ItemPosition switchPosition = parentCasePosition.getParentItemPosition();
							final ItemPosition destinationPosition = switchPosition.getParentItemPosition();
							final InitializationSwitchFacade switchFacade = (InitializationSwitchFacade) switchPosition
									.getItem();
							final Facade destinationFacade = switchFacade.getParent();
							final String firstInitializerFacadeString = ((Facade) firstItemPosition.getItem())
									.toString();
							result.add(new AbstractDynamicListAction() {

								@Override
								public String getName() {
									return "moveOutOfParentSwitchCases";
								}

								@Override
								public String getCaption() {
									return "Move Out Of Parent Switch/Cases";
								}

								@Override
								public DisplayMode getDisplayMode() {
									return DisplayMode.CONTEXT_MENU;
								}

								@Override
								public Object invoke(Object object, InvocationData invocationData) {
									List<Facade> initializerFacades = selection.stream()
											.map(itemPosition -> (Facade) itemPosition.getItem())
											.collect(Collectors.toList());
									List<Facade> managedFacades = switchFacade.getManagedInitializerFacades();
									InitializationCase destination = (InitializationCase) destinationFacade
											.getUnderlying();
									for (Facade initializerFacade : initializerFacades) {
										if (initializerFacade instanceof ParameterInitializerFacade) {
											ParameterInitializer initializer = ((ParameterInitializerFacade) initializerFacade)
													.getUnderlying();
											destination.getParameterInitializers().add(initializer);
										} else if (initializerFacade instanceof FieldInitializerFacade) {
											((FieldInitializerFacade) initializerFacade).setConcrete(true);
											FieldInitializer initializer = ((FieldInitializerFacade) initializerFacade)
													.getUnderlying();
											destination.getFieldInitializers().add(initializer);
										} else if (initializerFacade instanceof ListItemInitializerFacade) {
											((ListItemInitializerFacade) initializerFacade).setConcrete(true);
											ListItemInitializer initializer = ((ListItemInitializerFacade) initializerFacade)
													.getUnderlying();
											destination.getListItemInitializers().add(initializer);
										} else if (initializerFacade instanceof InitializationSwitchFacade) {
											InitializationSwitch initializer = ((InitializationSwitchFacade) initializerFacade)
													.getUnderlying();
											destination.getInitializationSwitches().add(initializer);
										} else {
											throw new UnexpectedError();
										}
										if (!managedFacades.remove(initializerFacade)) {
											throw new UnexpectedError();
										}
									}
									switchFacade.setManagedInitializerFacades(managedFacades);
									return null;
								}

								@Override
								public List<ItemPosition> getPostSelection() {
									return destinationPosition.getSubItemPositions().stream()
											.filter(itemPosition -> ((Facade) itemPosition.getItem()).toString()
													.equals(firstInitializerFacadeString))
											.collect(Collectors.toList());
								}

							});
						}
					}
				}
				return result;
			}

			@Override
			protected String toString(ITypeInfo type, Object object) {
				if (object instanceof OperationMetadata) {
					return ((OperationMetadata) object).getOperationTypeName();
				}
				if (object instanceof Throwable) {
					return object.toString();
				}
				return super.toString(type, object);
			}

			@Override
			protected boolean onFormVisibilityChange(ITypeInfo type, Object object, boolean visible) {
				if (visible) {
					if (object instanceof Plan) {
						displayedPlan = (Plan) object;
						return true;
					} else if (object instanceof Step) {
						displayedStep = (Step) object;
						return true;
					} else if (object instanceof Transition) {
						displayedTransition = (Transition) object;
						return true;
					} else if (object instanceof RootInstanceBuilderFacade) {
						displayedRootInstanceBuilderFacade = (RootInstanceBuilderFacade) object;
						return true;
					}
				} else {
					if (object instanceof Debugger) {
						((Debugger) object).deactivatePlans();
						((Debugger) object).stopExecutions();
						return true;
					}
				}
				return super.onFormVisibilityChange(type, object, visible);
			}

			@Override
			protected List<IFieldInfo> getFields(ITypeInfo type) {
				Class<?> objectClass;
				try {
					objectClass = ClassUtils.getCachedClassForName(type.getName());
				} catch (ClassNotFoundException e) {
					objectClass = null;
				}
				if (type.getName().equals(Solution.class.getName())) {
					List<IFieldInfo> result = new ArrayList<IFieldInfo>(super.getFields(type));
					result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {

						@Override
						public String getName() {
							return "sidePaneValue";
						}

						@Override
						public String getCaption() {
							return ReflectionUIUtils.identifierToCaption(getName());
						}

						@Override
						public Object getValue(Object object) {
							if ("env".equals(sidePaneValueName)) {
								return System.getenv();
							} else if ("logs".equals(sidePaneValueName)) {
								return MiscUtils.getPrintedStackTrace(new Exception());
							} else {
								return null;
							}
						}

						@Override
						public ITypeInfo getType() {
							return getTypeInfo(new JavaTypeInfoSource(Object.class, null));
						}

						@Override
						public boolean isTransient() {
							return true;
						}

						@Override
						public boolean isGetOnly() {
							return true;
						}

					});
					return result;
				} else if ((objectClass != null) && Throwable.class.isAssignableFrom(objectClass)) {
					List<IFieldInfo> result = new ArrayList<IFieldInfo>();
					for (IFieldInfo field : super.getFields(type)) {
						if (field.getName().equals("cause")) {
							field = new ValueAsListFieldInfo(JESBReflectionUI.this, field, type) {

								@Override
								public String getCaption() {
									return "Cause(s)";
								}

								@Override
								protected IListTypeInfo createListType() {
									return new ListTypeInfo() {
										@Override
										public IListItemDetailsAccessMode getDetailsAccessMode() {
											return new EmbeddedItemDetailsAccessMode();
										}

										@Override
										public IListStructuralInfo getStructuralInfo() {
											return new DefaultListStructuralInfo(reflectionUI) {

												@Override
												public IFieldInfo getItemSubListField(ItemPosition itemPosition) {
													return ReflectionUIUtils.findInfoByName(reflectionUI
															.getTypeInfo(reflectionUI
																	.getTypeInfoSource(itemPosition.getItem()))
															.getFields(), "cause");
												}

												@Override
												public IInfoFilter getItemDetailsInfoFilter(ItemPosition itemPosition) {
													return new InfoFilterProxy(
															super.getItemDetailsInfoFilter(itemPosition)) {

														@Override
														public IFieldInfo apply(IFieldInfo field) {
															if (field.getName().equals("cause")) {
																field = null;
															}
															return field;
														}

													};
												}

												@Override
												public int getHeight() {
													return 200;
												}

											};
										}
									};
								}

							};
						}
						result.add(field);
					}
					return result;
				} else if (type.getName().equals(Plan.class.getName())) {
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
							return new PlanDiagram.Source((Plan) object);
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
							return new PlanDiagramPalette.Source((Plan) object);
						}

						@Override
						public ITypeInfo getType() {
							return getTypeInfo(new JavaTypeInfoSource(PlanDiagramPalette.Source.class, null));
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
							return (DragIntent) diagramDragIntentByPlan.getOrDefault((Plan) object, DragIntent.MOVE);
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
				} else if (type.getName().equals(PlanExecutor.class.getName())
						|| type.getName().equals(PlanExecutor.SubPlanExecutor.class.getName())) {
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
							return new DebugPlanDiagram.Source(((PlanExecutor) object).getPlan());
						}

						@Override
						public ITypeInfo getType() {
							return getTypeInfo(new JavaTypeInfoSource(DebugPlanDiagram.Source.class, null));
						}

					});
					return result;
				} else if (type.getName().equals(RootInstanceBuilderFacade.class.getName())) {
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
							if (displayedPlan == null) {
								return null;
							}
							Step currentStep;
							if (displayedPlan.getOutputBuilder() == ((RootInstanceBuilderFacade) object)
									.getUnderlying()) {
								currentStep = null;
							} else {
								currentStep = displayedStep;
							}
							return new PathOptionsProvider(
									displayedPlan.getValidationContext(currentStep).getVariableDeclarations())
											.getRootPathNodes();
						}

						@Override
						public ITypeInfo getType() {
							return getTypeInfo(new JavaTypeInfoSource(List.class, new Class<?>[] { PathNode.class },
									new SpecificitiesIdentifier(RootInstanceBuilderFacade.class.getName(), getName())));
						}

					});
					result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {
						@Override
						public String getName() {
							return "mappings";
						}

						@Override
						public String getCaption() {
							return "Mappings";
						}

						@Override
						public Object getValue(Object object) {
							return new MappingsControl.Source((RootInstanceBuilderFacade) object, displayedPlan,
									displayedStep);
						}

						@Override
						public ITypeInfo getType() {
							return getTypeInfo(new JavaTypeInfoSource(MappingsControl.Source.class,
									new SpecificitiesIdentifier(RootInstanceBuilderFacade.class.getName(), getName())));
						}

					});
					return result;
				} else if (type.getName().equals(ListItemReplicationFacade.class.getName())) {
					List<IFieldInfo> result = new ArrayList<IFieldInfo>(super.getFields(type));
					result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {
						@Override
						public String getName() {
							return "inferredIterationVariableTypeName";
						}

						@Override
						public String getCaption() {
							return "Inferred Iteration Variable Type";
						}

						@Override
						public Object getValue(Object object) {
							if (displayedPlan == null) {
								return null;
							}
							if (displayedRootInstanceBuilderFacade == null) {
								return null;
							}
							Step currentStep;
							if (displayedPlan.getOutputBuilder() == displayedRootInstanceBuilderFacade
									.getUnderlying()) {
								currentStep = null;
							} else {
								currentStep = displayedStep;
							}
							ITypeInfo variableType = ((ListItemReplicationFacade) object)
									.guessIterationVariableTypeInfo(
											displayedPlan.getValidationContext(currentStep).getVariableDeclarations());
							if (variableType != null) {
								return variableType.getName();
							} else {
								return Object.class.getName();
							}
						}

						@Override
						public ITypeInfo getType() {
							return getTypeInfo(new JavaTypeInfoSource(String.class,
									new SpecificitiesIdentifier(ListItemReplicationFacade.class.getName(), getName())));
						}

					});
					return result;
				} else {
					return super.getFields(type);
				}
			}

			@Override
			protected List<IMethodInfo> getMethods(ITypeInfo type) {
				Class<?> objectClass;
				try {
					objectClass = ClassUtils.getCachedClassForName(type.getName());
				} catch (ClassNotFoundException e) {
					objectClass = null;
				}
				if (type.getName().equals(Solution.class.getName())) {
					List<IMethodInfo> result = new ArrayList<IMethodInfo>(super.getMethods(type));
					result.add(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

						@Override
						public String getSignature() {
							return ReflectionUIUtils.buildMethodSignature(this);
						}

						@Override
						public String getName() {
							return "changeSidePanelValue";
						}

						@Override
						public String getCaption() {
							return ReflectionUIUtils.identifierToCaption(getName());
						}

						@Override
						public List<IParameterInfo> getParameters() {
							return Collections
									.singletonList(new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

										@Override
										public String getName() {
											return "newSidePanelValueName";
										}

										@Override
										public String getCaption() {
											return ReflectionUIUtils.identifierToCaption(getName());
										}

										@Override
										public ITypeInfo getType() {
											return getTypeInfo(new JavaTypeInfoSource(String.class, null));
										}
									});
						}

						@Override
						public Object invoke(Object object, InvocationData invocationData) {
							String newSidePaneValueName = (String) invocationData.getParameterValue(0);
							if (newSidePaneValueName.equals(sidePaneValueName)) {
								sidePaneValueName = null;
							} else {
								sidePaneValueName = newSidePaneValueName;
							}
							return null;
						}

						@Override
						public boolean isReadOnly() {
							return true;
						}
					});
					return result;
				} else if ((objectClass != null) && Function.class.isAssignableFrom(objectClass)) {
					List<IMethodInfo> result = new ArrayList<IMethodInfo>(super.getMethods(type));
					result.add(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

						@Override
						public String getSignature() {
							return ReflectionUIUtils.buildMethodSignature(this);
						}

						@Override
						public String getName() {
							return "edit";
						}

						@Override
						public String getCaption() {
							return "Edit...";
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
							if (object instanceof InstantiationFunction) {
								InstantiationFunction function = (InstantiationFunction) object;
								Facade parentFacade = displayedRootInstanceBuilderFacade
										.findInstantiationFunctionParentFacade(function);
								Step currentStep;
								if (displayedPlan.getOutputBuilder() == displayedRootInstanceBuilderFacade
										.getUnderlying()) {
									currentStep = null;
								} else {
									currentStep = displayedStep;
								}
								List<VariableDeclaration> baseVariableDeclarations = displayedPlan
										.getValidationContext(currentStep).getVariableDeclarations();
								InstantiationFunctionCompilationContext compilationContext = new InstantiationFunctionCompilationContext(
										baseVariableDeclarations, parentFacade);
								return new FunctionEditor(function, compilationContext.getPrecompiler(),
										compilationContext.getVariableDeclarations(function),
										compilationContext.getFunctionReturnType(function));
							} else if (object instanceof Transition.IfCondition) {
								return new FunctionEditor((Transition.IfCondition) object, null,
										displayedPlan.getTransitionContextVariableDeclarations(displayedTransition),
										boolean.class);
							} else if ((displayedStep instanceof LoopCompositeStep)
									&& (((LoopCompositeStep) displayedStep).getOperationBuilder()
											.getLoopEndCondition() == object)) {
								return new FunctionEditor((Function) object, null, ((LoopCompositeStep) displayedStep)
										.getLoopEndConditionVariableDeclarations(displayedPlan), boolean.class);
							} else {
								throw new UnexpectedError();
							}
						}

						@Override
						public boolean isReadOnly() {
							return true;
						}
					});
					return result;
				} else if (type.getName().equals(LoopOperation.Builder.class.getName())) {
					List<IMethodInfo> result = new ArrayList<IMethodInfo>(super.getMethods(type));
					result.add(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

						@Override
						public String getSignature() {
							return ReflectionUIUtils.buildMethodSignature(this);
						}

						@Override
						public String getName() {
							return "retrieveResultsCollectionConfigurationEntries";
						}

						@Override
						public String getCaption() {
							return ReflectionUIUtils.identifierToCaption(getName());
						}

						@Override
						public ITypeInfo getReturnValueType() {
							return getTypeInfo(new JavaTypeInfoSource(List.class,
									new Class<?>[] { ResultsCollectionConfigurationEntry.class }, null));
						}

						@Override
						public Object invoke(Object object, InvocationData invocationData) {
							return ((LoopOperation.Builder) object)
									.retrieveResultsCollectionConfigurationEntries(displayedPlan, displayedStep);
						}
					});
					result.add(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

						@Override
						public String getSignature() {
							return ReflectionUIUtils.buildMethodSignature(this);
						}

						@Override
						public String getName() {
							return "updateResultsCollectionConfigurationEntries";
						}

						@Override
						public String getCaption() {
							return ReflectionUIUtils.identifierToCaption(getName()) + "...";
						}

						@Override
						public List<IParameterInfo> getParameters() {
							return Collections
									.singletonList(new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

										@Override
										public String getName() {
											return "entries";
										}

										@Override
										public String getCaption() {
											return ReflectionUIUtils.identifierToCaption(getName());
										}

										@Override
										public ITypeInfo getType() {
											return getTypeInfo(new JavaTypeInfoSource(List.class,
													new Class<?>[] { ResultsCollectionConfigurationEntry.class },
													null));
										}
									});
						}

						@SuppressWarnings("unchecked")
						@Override
						public Object invoke(Object object, InvocationData invocationData) {
							((LoopOperation.Builder) object).updateResultsCollectionConfigurationEntries(
									(List<ResultsCollectionConfigurationEntry>) invocationData.getParameterValue(0),
									displayedPlan, displayedStep);
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
				if (type.getName().equals(OperationBuilder.class.getName())) {
					List<ITypeInfo> result = new ArrayList<ITypeInfo>();
					for (OperationMetadata operationMetadata : OPERATION_METADATAS) {
						result.add(getTypeInfo(
								new JavaTypeInfoSource(operationMetadata.getOperationBuilderClass(), null)));
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
				if (type.getName().equals(ReflectionUIError.class.getName())) {
					return "Error";
				}
				for (OperationMetadata operationMetadata : OPERATION_METADATAS) {
					if (operationMetadata.getOperationBuilderClass().getName().equals(type.getName())) {
						return operationMetadata.getOperationTypeName();
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
				for (OperationMetadata operationMetadata : OPERATION_METADATAS) {
					if (operationMetadata.getOperationBuilderClass().getName().equals(type.getName())) {
						return operationMetadata.getOperationIconImagePath();
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
				if (object instanceof StepCrossing) {
					return MiscUtils.getIconImagePath(((StepCrossing) object).getStep());
				}
				if (object instanceof PlanActivator) {
					return ReflectionUIUtils.getIconImagePath(JESBReflectionUI.this,
							((PlanActivator) object).getPlan());
				}
				if (object instanceof PlanExecutor) {
					PlanExecutor executor = (PlanExecutor) object;
					if (executor.isActive()) {
						return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
								JESBReflectionUI.class.getPackage().getName().replace(".", "/") + "/running.png"));
					} else {
						if (executor.getExecutionError() == null) {
							return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
									JESBReflectionUI.class.getPackage().getName().replace(".", "/") + "/success.png"));
						} else {
							return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
									JESBReflectionUI.class.getPackage().getName().replace(".", "/") + "/failure.png"));
						}
					}
				}
				if (object instanceof Element) {
					if (((Element) object).getOptionality() == null) {
						return getIconImagePath(
								getTypeInfo(new JavaTypeInfoSource(ParameterInitializerFacade.class, null)), null);
					} else {
						return getIconImagePath(getTypeInfo(new JavaTypeInfoSource(FieldInitializerFacade.class, null)),
								null);
					}
				}
				return super.getIconImagePath(type, object);
			}

			@Override
			protected boolean isHidden(IFieldInfo field, ITypeInfo objectType) {
				Class<?> objectClass;
				try {
					objectClass = ClassUtils.getCachedClassForName(objectType.getName());
				} catch (ClassNotFoundException e) {
					objectClass = null;
				}
				if ((objectClass != null) && Throwable.class.isAssignableFrom(objectClass)) {
					if (!field.getName().equals("message") && !field.getName().equals("cause")) {
						for (IFieldInfo throwableField : ReflectionUI.getDefault()
								.getTypeInfo(new JavaTypeInfoSource(Throwable.class, null)).getFields()) {
							if (field.getName().equals(throwableField.getName())) {
								return true;
							}
						}
					}
				}
				if ((objectClass != null) && ActivationStrategy.class.isAssignableFrom(objectClass)) {
					if (field.getName().equals("inputClass")) {
						return true;
					}
					if (field.getName().equals("outputClass")) {
						return true;
					}
					if (field.getName().equals("automaticTriggerReady")) {
						return true;
					}
					if (field.getName().equals("automaticallyTriggerable")) {
						return true;
					}
				}
				return super.isHidden(field, objectType);
			}

			@Override
			protected boolean isHidden(IMethodInfo method, ITypeInfo objectType) {
				Class<?> objectClass;
				try {
					objectClass = ClassUtils.getCachedClassForName(objectType.getName());
				} catch (ClassNotFoundException e) {
					objectClass = null;
				}
				if ((objectClass != null) && Throwable.class.isAssignableFrom(objectClass)) {
					for (IMethodInfo throwableMethod : getTypeInfo(new JavaTypeInfoSource(Throwable.class, null))
							.getMethods()) {
						if (method.getSignature().equals(throwableMethod.getSignature())) {
							return true;
						}
					}
				}
				if ((objectClass != null) && Asset.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
							Arrays.asList(boolean.class.getName())))) {
						return true;
					}
				}
				if ((objectClass != null) && Step.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
							Arrays.asList(boolean.class.getName(), Plan.class.getName())))) {
						return true;
					}
				} else if ((objectClass != null) && Transition.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
							Arrays.asList(boolean.class.getName(), Plan.class.getName())))) {
						return true;
					}
				} else if ((objectClass != null) && Transition.Condition.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
							Arrays.asList(List.class.getName())))) {
						return true;
					}
				}
				if ((objectClass != null) && OperationBuilder.class.isAssignableFrom(objectClass)) {
					if (method.getSignature()
							.matches(MiscUtils
									.escapeRegex(ReflectionUIUtils.buildMethodSignature(Operation.class.getName(),
											"build", Arrays.asList(Plan.ExecutionContext.class.getName(),
													Plan.ExecutionInspector.class.getName())))
									.replace(MiscUtils.escapeRegex(Operation.class.getName()), ".*"))) {
						return true;
					}
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature(Class.class.getName(),
							"getOperationResultClass", Arrays.asList(Plan.class.getName(), Step.class.getName())))) {
						return true;
					}
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
							Arrays.asList(boolean.class.getName(), Plan.class.getName(), Step.class.getName())))) {
						return true;
					}
				}
				if ((objectClass != null) && Facade.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
							Arrays.asList(boolean.class.getName(), List.class.getName())))) {
						return true;
					}
				}
				if ((objectClass != null) && Structure.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
							Arrays.asList(boolean.class.getName())))) {
						return true;
					}
				}
				if ((objectClass != null) && Structure.Element.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
							Arrays.asList(boolean.class.getName())))) {
						return true;
					}
				}
				if ((objectClass != null) && ActivationStrategy.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
							Arrays.asList(boolean.class.getName(), Plan.class.getName())))) {
						return true;
					}
				}
				if ((objectClass != null) && ActivationStrategy.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void",
							"initializeAutomaticTrigger", Arrays.asList(ActivationHandler.class.getName())))) {
						return true;
					}
				}
				if ((objectClass != null) && ActivationStrategy.class.isAssignableFrom(objectClass)) {
					if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void",
							"finalizeAutomaticTrigger", Arrays.asList()))) {
						return true;
					}
				}
				return super.isHidden(method, objectType);
			}

			@Override
			protected void validate(ITypeInfo type, Object object, ValidationSession session) throws Exception {
				if (object instanceof Plan) {
					session.put(CURRENT_VALIDATION_PLAN_KEY, object);
				}
				if (object instanceof Step) {
					session.put(CURRENT_VALIDATION_STEP_KEY, object);
				}
				if (object instanceof Transition) {
					session.put(CURRENT_VALIDATION_TRANSITION_KEY, object);
				}
				Class<?> objectClass;
				try {
					objectClass = ClassUtils.getCachedClassForName(type.getName());
				} catch (ClassNotFoundException e) {
					objectClass = null;
				}
				if ((objectClass != null) && Asset.class.isAssignableFrom(objectClass)) {
					try {
						((Asset) object).validate(false);
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else if ((objectClass != null) && Step.class.isAssignableFrom(objectClass)) {
					try {
						((Step) object).validate(false, getCurrentValidationPlan(session));
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else if ((objectClass != null) && Transition.class.isAssignableFrom(objectClass)) {
					try {
						((Transition) object).validate(false, getCurrentValidationPlan(session));
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else if ((objectClass != null) && Transition.Condition.class.isAssignableFrom(objectClass)) {
					try {
						((Transition.Condition) object).validate(getCurrentValidationPlan(session)
								.getTransitionContextVariableDeclarations(getCurrentValidationTransition(session)));
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else if ((objectClass != null) && OperationBuilder.class.isAssignableFrom(objectClass)) {
					try {
						((OperationBuilder) object).validate(false, getCurrentValidationPlan(session),
								getCurrentValidationStep(session));
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else if ((objectClass != null) && Facade.class.isAssignableFrom(objectClass)) {
					Step step = getCurrentValidationStep(session);
					Plan plan = getCurrentValidationPlan(session);
					RootInstanceBuilderFacade rootInstanceBuilderFacade = (RootInstanceBuilderFacade) Facade
							.getRoot((Facade) object);
					step = (plan.getOutputBuilder() == rootInstanceBuilderFacade.getUnderlying()) ? null : step;
					try {
						((Facade) object).validate(false, plan.getValidationContext(step).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else if ((objectClass != null) && ListItemReplicationFacade.class.isAssignableFrom(objectClass)) {
					Step step = getCurrentValidationStep(session);
					Plan plan = getCurrentValidationPlan(session);
					RootInstanceBuilderFacade rootInstanceBuilderFacade = (RootInstanceBuilderFacade) Facade
							.getRoot(((ListItemReplicationFacade) object).getListItemInitializerFacade());
					step = (plan.getOutputBuilder() == rootInstanceBuilderFacade.getUnderlying()) ? null : step;
					try {
						((ListItemReplicationFacade) object).validate(false,
								plan.getValidationContext(step).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else if ((objectClass != null) && Structure.class.isAssignableFrom(objectClass)) {
					try {
						((Structure) object).validate(false);
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else if ((objectClass != null) && Structure.Element.class.isAssignableFrom(objectClass)) {
					try {
						((Structure.Element) object).validate(false);
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else if ((objectClass != null) && ActivationStrategy.class.isAssignableFrom(objectClass)) {
					Plan plan = getCurrentValidationPlan(session);
					try {
						((ActivationStrategy) object).validate(false, plan);
					} catch (ValidationError e) {
						throw new ReflectionUIError(e);
					}
				} else {
					super.validate(type, object, session);
				}
			}

			Plan getCurrentValidationPlan(ValidationSession session) {
				Plan result = (Plan) session.get(CURRENT_VALIDATION_PLAN_KEY);
				if (result == null) {
					result = displayedPlan;
				}
				return result;
			}

			Step getCurrentValidationStep(ValidationSession session) {
				Step result = (Step) session.get(CURRENT_VALIDATION_STEP_KEY);
				if (result == null) {
					result = displayedStep;
				}
				return result;
			}

			Transition getCurrentValidationTransition(ValidationSession session) {
				Transition result = (Transition) session.get(CURRENT_VALIDATION_TRANSITION_KEY);
				if (result == null) {
					result = displayedTransition;
				}
				return result;
			}

			@Override
			protected boolean isModificationStackAccessible(ITypeInfo type) {
				try {
					Class<?> objectClass = ClassUtils.getCachedClassForName(type.getName());
					if (Throwable.class.isAssignableFrom(objectClass)) {
						return false;
					}
				} catch (ClassNotFoundException e) {
				}
				return super.isModificationStackAccessible(type);
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