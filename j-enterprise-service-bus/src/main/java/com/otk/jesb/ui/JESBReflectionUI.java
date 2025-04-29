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
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.PathOptionsProvider;
import com.otk.jesb.Plan;
import com.otk.jesb.Step;
import com.otk.jesb.StepOccurrence;
import com.otk.jesb.Debugger.PlanActivator;
import com.otk.jesb.Debugger.PlanExecutor;
import com.otk.jesb.LoopCompositeStep.LoopActivity;
import com.otk.jesb.LoopCompositeStep.LoopActivity.Builder.ResultsCollectionConfigurationEntry;
import com.otk.jesb.Structure.Element;
import com.otk.jesb.Transition;
import com.otk.jesb.ValidationError;
import com.otk.jesb.activity.ActivityBuilder;
import com.otk.jesb.activity.ActivityMetadata;
import com.otk.jesb.activity.builtin.CallSOAPWebServiceActivity;
import com.otk.jesb.activity.builtin.ExecutePlanActivity;
import com.otk.jesb.activity.builtin.JDBCQueryActivity;
import com.otk.jesb.activity.builtin.JDBCUpdateActivity;
import com.otk.jesb.activity.builtin.ReadFileActivity;
import com.otk.jesb.activity.builtin.SleepActivity;
import com.otk.jesb.activity.builtin.WriteFileActivity;
import com.otk.jesb.diagram.DragIntent;
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
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.resource.builtin.WSDL;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.field.FieldInfoProxy;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.parameter.ParameterInfoProxy;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.item.ItemPosition;
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

	public static final List<ActivityMetadata> ACTIVITY_METADATAS = Arrays.asList(new SleepActivity.Metadata(),
			new ExecutePlanActivity.Metadata(), new ReadFileActivity.Metadata(), new WriteFileActivity.Metadata(),
			new JDBCQueryActivity.Metadata(), new JDBCUpdateActivity.Metadata(),
			new CallSOAPWebServiceActivity.Metadata());
	public static final List<ActivityMetadata> COMPOSITE_METADATAS = Arrays.asList(new LoopActivity.Metadata());
	public static final List<ResourceMetadata> RESOURCE_METADATAS = Arrays.asList(new JDBCConnection.Metadata(),
			new WSDL.Metadata());

	private static WeakHashMap<RootInstanceBuilder, ByteArrayOutputStream> rootInitializerStoreByBuilder = new WeakHashMap<RootInstanceBuilder, ByteArrayOutputStream>();
	static WeakHashMap<Plan, DragIntent> diagramDragIntentByPlan = new WeakHashMap<Plan, DragIntent>();

	private Plan currentPlan;
	private Step currentStep;
	private Transition currentTransition;

	public static void backupRootInstanceBuilderState(RootInstanceBuilder rootInstanceBuilder) {
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

	public static Runnable getRootInstanceBuilderStateRestorationJob(RootInstanceBuilder rootInstanceBuilder) {
		if (!rootInitializerStoreByBuilder.containsKey(rootInstanceBuilder)) {
			throw new AssertionError();
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
					throw new AssertionError(e);
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
											throw new AssertionError();
										}
										if (!managedFacades.remove(initializerFacade)) {
											throw new AssertionError();
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
				if (object instanceof ActivityMetadata) {
					return ((ActivityMetadata) object).getActivityTypeName();
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
						currentPlan = (Plan) object;
						return true;
					} else if (object instanceof Step) {
						currentStep = (Step) object;
						return true;
					} else if (object instanceof Transition) {
						currentTransition = (Transition) object;
						return true;
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
				} else if (type.getName().equals(RootInstanceBuilderFacade.class.getName()))

				{
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
							if (currentPlan == null) {
								return null;
							}
							return new PathOptionsProvider(currentPlan
									.getValidationContext(
											(object == currentPlan.getOutputBuilder()) ? null : currentStep)
									.getVariableDeclarations()).getRootPathNodes();
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
							return new MappingsControl.Source();
						}

						@Override
						public ITypeInfo getType() {
							return getTypeInfo(new JavaTypeInfoSource(MappingsControl.Source.class,
									new SpecificitiesIdentifier(RootInstanceBuilderFacade.class.getName(), getName())));
						}

					});
					return result;
				} else {
					return super.getFields(type);
				}
			}

			@Override
			protected List<IMethodInfo> getMethods(ITypeInfo type) {
				if (type.getName().equals(InstantiationFunction.class.getName())) {
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
							ValidationContext validationContext;
							InstantiationFunctionCompilationContext compilationContext;
							compilationContext = currentStep.getActivityBuilder().findFunctionCompilationContext(
									(InstantiationFunction) object, currentStep, currentPlan);
							if (compilationContext == null) {
								validationContext = currentPlan.getValidationContext(null);
								compilationContext = currentPlan.getOutputBuilder().getFacade()
										.findFunctionCompilationContext((InstantiationFunction) object,
												validationContext.getVariableDeclarations());
								if (compilationContext == null) {
									throw new AssertionError();
								}
							}
							return new FunctionEditor((InstantiationFunction) object,
									compilationContext.getPrecompiler(), compilationContext.getVariableDeclarations(),
									compilationContext.getFunctionReturnType());
						}

						@Override
						public boolean isReadOnly() {
							return true;
						}
					});
					return result;
				} else if (type.getName().equals(Transition.IfCondition.class.getName())) {
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
							return new FunctionEditor((Transition.IfCondition) object, null,
									currentPlan.getTransitionContextVariableDeclarations(currentTransition),
									boolean.class);
						}

						@Override
						public boolean isReadOnly() {
							return true;
						}
					});
					return result;
				} else if (type.getName().equals(LoopActivity.Builder.class.getName())) {
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
							return ((LoopActivity.Builder) object)
									.retrieveResultsCollectionConfigurationEntries(currentPlan, currentStep);
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
							((LoopActivity.Builder) object).updateResultsCollectionConfigurationEntries(
									(List<ResultsCollectionConfigurationEntry>) invocationData.getParameterValue(0),
									currentPlan, currentStep);
							return null;
						}
					});
					result.add(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

						@Override
						public String getSignature() {
							return ReflectionUIUtils.buildMethodSignature(this);
						}

						@Override
						public String getName() {
							return "validate";
						}

						@Override
						public String getCaption() {
							return ReflectionUIUtils.identifierToCaption(getName());
						}

						@Override
						public Object invoke(Object object, InvocationData invocationData) {
							try {
								((LoopActivity.Builder) object).validate(currentPlan, currentStep);
							} catch (ValidationError e) {
								throw new ReflectionUIError(e);
							}
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
						result.add(
								getTypeInfo(new JavaTypeInfoSource(activityMetadata.getActivityBuilderClass(), null)));
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
						return getIconImagePath(getTypeInfo(new JavaTypeInfoSource(FieldInitializerFacade.class, null)),
								null);
					}
				}
				return super.getIconImagePath(type, object);
			}

			@Override
			protected boolean isHidden(IFieldInfo field, ITypeInfo objectType) {
				if (!field.getName().equals("message") && !field.getName().equals("cause")) {
					try {
						Class<?> objectClass = ClassUtils.getCachedClassForName(objectType.getName());
						if (Throwable.class.isAssignableFrom(objectClass)) {
							for (IFieldInfo throwableField : getTypeInfo(new JavaTypeInfoSource(Throwable.class, null))
									.getFields()) {
								if (field.getName().equals(throwableField.getName())) {
									return true;
								}
							}
						}
					} catch (ClassNotFoundException e) {
					}
				}
				return super.isHidden(field, objectType);
			}

			@Override
			protected boolean isHidden(IMethodInfo method, ITypeInfo objectType) {
				try {
					Class<?> objectClass = ClassUtils.getCachedClassForName(objectType.getName());
					if (Throwable.class.isAssignableFrom(objectClass)) {
						if (!method.getSignature().equals("void printStackTrace()")) {
							for (IMethodInfo throwableMethod : getTypeInfo(
									new JavaTypeInfoSource(Throwable.class, null)).getMethods()) {
								if (method.getSignature().equals(throwableMethod.getSignature())) {
									return true;
								}
							}
						}
					}
					if (ActivityBuilder.class.isAssignableFrom(objectClass)) {
						if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature(
								InstantiationFunctionCompilationContext.class.getName(),
								"findFunctionCompilationContext", Arrays.asList(InstantiationFunction.class.getName(),
										Step.class.getName(), Plan.class.getName())))) {
							return true;
						}
						if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature(Class.class.getName(),
								"getActivityResultClass", Arrays.asList(Plan.class.getName(), Step.class.getName())))) {
							return true;
						}
					}
				} catch (ClassNotFoundException e) {
				}
				return super.isHidden(method, objectType);
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