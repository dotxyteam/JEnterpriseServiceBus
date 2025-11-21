package com.otk.jesb.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.DropMode;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import com.otk.jesb.Debugger;
import com.otk.jesb.Expression;
import com.otk.jesb.FunctionEditor;
import com.otk.jesb.JESB;
import com.otk.jesb.Log;
import com.otk.jesb.PathOptionsProvider;
import com.otk.jesb.PluginBuilder;
import com.otk.jesb.PotentialError;
import com.otk.jesb.Reference;
import com.otk.jesb.Session;
import com.otk.jesb.Structure;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.Structure.Element;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.Variant;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.activation.ActivatorStructure;
import com.otk.jesb.activation.builtin.HTTPRequestReceiver;
import com.otk.jesb.Debugger.PlanActivation;
import com.otk.jesb.Debugger.PlanExecutor;
import com.otk.jesb.EnvironmentSettings.EnvironmentVariable;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FacadeOutline;
import com.otk.jesb.instantiation.FieldInitializer;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.InitializationCase;
import com.otk.jesb.instantiation.InitializationCaseFacade;
import com.otk.jesb.instantiation.InitializationSwitch;
import com.otk.jesb.instantiation.InitializationSwitchFacade;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.InstantiationFunction;
import com.otk.jesb.instantiation.InstantiationFunctionCompilationContext;
import com.otk.jesb.instantiation.ListItemInitializer;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.ListItemReplicationFacade;
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.instantiation.ValueMode;
import com.otk.jesb.meta.Date;
import com.otk.jesb.meta.DateTime;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.operation.OperationStructureBuilder;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.resource.ResourceStructure;
import com.otk.jesb.resource.builtin.HTTPServer;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.CompositeStep;
import com.otk.jesb.solution.Folder;
import com.otk.jesb.solution.LoopCompositeStep;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.PlanElement;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.StepCrossing;
import com.otk.jesb.solution.Transition;
import com.otk.jesb.solution.CompositeStep.CompositeStepMetadata;
import com.otk.jesb.solution.LoopCompositeStep.LoopOperation;
import com.otk.jesb.solution.LoopCompositeStep.LoopOperation.Builder.ResultsCollectionConfigurationEntry;
import com.otk.jesb.ui.diagram.DragIntent;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.FadingPanel;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.SquigglePainter;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.control.FieldControlDataProxy;
import xy.reflect.ui.control.FieldControlInputProxy;
import xy.reflect.ui.control.IFieldControlData;
import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.plugin.IFieldControlPlugin;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.NullableControl;
import xy.reflect.ui.control.swing.TextControl;
import xy.reflect.ui.control.swing.builder.AbstractEditorBuilder;
import xy.reflect.ui.control.swing.builder.AbstractEditorFormBuilder;
import xy.reflect.ui.control.swing.customizer.CustomizationController;
import xy.reflect.ui.control.swing.customizer.CustomizationTools;
import xy.reflect.ui.control.swing.customizer.CustomizationToolsUI;
import xy.reflect.ui.control.swing.customizer.CustomizingFieldControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.customizer.CustomizingMethodControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.MultiSwingCustomizer;
import xy.reflect.ui.control.swing.plugin.DatePickerPlugin;
import xy.reflect.ui.control.swing.plugin.DateTimePickerPlugin;
import xy.reflect.ui.control.swing.plugin.EditorPlugin;
import xy.reflect.ui.control.swing.plugin.StyledTextPlugin;
import xy.reflect.ui.control.swing.plugin.ToggleButtonPlugin;
import xy.reflect.ui.control.swing.plugin.ToggleButtonPlugin.ToggleButtonConfiguration;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.renderer.SwingRenderer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.InfoCategory;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.ValidationSession;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.info.field.MembersCapsuleFieldInfo;
import xy.reflect.ui.info.field.ValueAsListFieldInfo;
import xy.reflect.ui.info.field.FieldInfoProxy;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.filter.IInfoFilter;
import xy.reflect.ui.info.filter.InfoFilterProxy;
import xy.reflect.ui.info.method.AbstractConstructorInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.parameter.ParameterInfoProxy;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.ITypeInfo.CategoriesStyle;
import xy.reflect.ui.info.type.ITypeInfo.FieldsLayout;
import xy.reflect.ui.info.type.ITypeInfo.IValidationJob;
import xy.reflect.ui.info.type.factory.EncapsulatedObjectFactory;
import xy.reflect.ui.info.type.factory.GenericEnumerationFactory;
import xy.reflect.ui.info.type.factory.IInfoProxyFactory;
import xy.reflect.ui.info.type.factory.InfoCustomizationsFactory;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;
import xy.reflect.ui.info.type.factory.InfoProxyFactoryChain;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.info.type.iterable.item.EmbeddedItemDetailsAccessMode;
import xy.reflect.ui.info.type.iterable.item.IListItemDetailsAccessMode;
import xy.reflect.ui.info.type.iterable.item.ItemPosition;
import xy.reflect.ui.info.type.iterable.structure.DefaultListStructuralInfo;
import xy.reflect.ui.info.type.iterable.structure.IListStructuralInfo;
import xy.reflect.ui.info.type.iterable.util.AbstractDynamicListAction;
import xy.reflect.ui.info.type.iterable.util.AbstractDynamicListProperty;
import xy.reflect.ui.info.type.iterable.util.DynamicListActionProxy;
import xy.reflect.ui.info.type.iterable.util.IDynamicListAction;
import xy.reflect.ui.info.type.iterable.util.IDynamicListProperty;
import xy.reflect.ui.info.type.source.ITypeInfoSource;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.info.type.source.PrecomputedTypeInfoSource;
import xy.reflect.ui.info.type.source.SpecificitiesIdentifier;
import xy.reflect.ui.undo.ListModificationFactory;
import xy.reflect.ui.util.Mapper;
import xy.reflect.ui.util.PrecomputedTypeInstanceWrapper;
import xy.reflect.ui.util.ReflectionUIError;
import xy.reflect.ui.util.ReflectionUIUtils;
import xy.reflect.ui.util.SystemProperties;
import xy.reflect.ui.util.ValidationErrorRegistry;

/**
 * Main graphical user interface rendering class of the application.
 * 
 * @author olitank
 *
 */
public class GUI extends MultiSwingCustomizer {

	public static final String UI_CUSTOMIZATIONS_METHOD_NAME = "customizeUI";
	public static final String GUI_CUSTOMIZATIONS_RESOURCE_DIRECTORY_PROPERTY_KEY = GUI.class.getPackage().getName()
			+ ".alternateUICustomizationsFileDirectory";

	private static final String CURRENT_ASSET_KEY = GUI.class.getName() + ".CURRENT_VALIDATION_ASSET_KEY";
	private static final String CURRENT_PLAN_ELEMENT_KEY = GUI.class.getName() + ".CURRENT_VALIDATION_PLAN_ELEMENT_KEY";
	private static final String CURRENT_INSTANTIATION_FACADE_KEY = GUI.class.getName()
			+ ".CURRENT_INSTANTIATION_FACADE_KEY";
	private static final String CURRENT_ACTIVATOR_KEY = GUI.class.getName() + ".CURRENT_VALIDATION_ACTIVATOR_KEY";

	private static WeakHashMap<RootInstanceBuilder, Object> rootInitializerBackupByBuilder = new WeakHashMap<RootInstanceBuilder, Object>();
	private static WeakHashMap<Plan, DragIntent> diagramDragIntentByPlan = new WeakHashMap<Plan, DragIntent>();

	private static Deque<Asset> stackOfCurrentAssets = new ArrayDeque<Asset>();
	private static Deque<PlanElement> stackOfCurrentPlanElements = new ArrayDeque<PlanElement>();
	private static Deque<Facade> stackOfCurrentInstantiationFacades = new ArrayDeque<Facade>();
	private static Deque<Activator> stackOfCurrentActivators = new ArrayDeque<Activator>();
	private static SidePaneValueName sidePaneValueName;
	private static boolean focusTrackingDisabled = false;
	private static List<Pair<ITypeInfo, Object>> lostFocusWhileTrackingDisabled = new ArrayList<Pair<ITypeInfo, Object>>();
	private static List<Pair<ITypeInfo, Object>> gainedFocusWhileTrackingDisabled = new ArrayList<Pair<ITypeInfo, Object>>();

	static {
		if (JESB.isDebugModeActive()) {
			System.setProperty(SystemProperties.DEBUG, Boolean.TRUE.toString());
		}
		Preferences.INSTANCE.getTheme().activate();
	}

	public static final String GUI_MAIN_CUSTOMIZATIONS_RESOURCE_NAME = "jesb.icu";

	public static GUI INSTANCE = new GUI();

	private List<SubSwingCustomizer> builtInSubCustomizers = new ArrayList<SubSwingCustomizer>();

	private GUI() {
		super(null, null);
		this.customizationsIdentifierSelector = new Function<Object, String>() {
			@Override
			public String apply(Object object) {
				return selectSubCustomizationsSwitch(object.getClass());
			}
		};
		preLoadBuiltInCustomizations();
	}

	private void preLoadBuiltInCustomizations() {
		List<Class<?>> mainCustomizionClasses = new ArrayList<Class<?>>();
		for (OperationMetadata<?> metadata : MiscUtils.BUILTIN_OPERATION_METADATAS) {
			mainCustomizionClasses.add(metadata.getOperationBuilderClass());
		}
		for (ActivatorMetadata metadata : MiscUtils.BUILTIN_ACTIVATOR__METADATAS) {
			mainCustomizionClasses.add(metadata.getActivatorClass());
		}
		for (ResourceMetadata metadata : MiscUtils.BUILTIN_RESOURCE_METADATAS) {
			mainCustomizionClasses.add(metadata.getResourceClass());
		}
		for (CompositeStepMetadata metadata : MiscUtils.BUILTIN_COMPOSITE_STEP_METADATAS) {
			mainCustomizionClasses.add(metadata.getCompositeStepClass());
		}
		for (Class<?> customizionClass : mainCustomizionClasses) {
			String customizationsIdentifier = selectSubCustomizationsSwitch(customizionClass);
			SubSwingCustomizer subCustomizer = obtainSubCustomizer(customizationsIdentifier);
			SubCustomizedUI subCustomizedUI = subCustomizer.getCustomizedUI();
			ITypeInfo customizedType = subCustomizedUI.getTypeInfo(subCustomizedUI.getTypeInfoSource(customizionClass));
			customizedType.getName();
			customizedType.getFields();
			customizedType.getMethods();
			customizedType.getConstructors();
			customizedType.getMenuModel();
			builtInSubCustomizers.add(subCustomizer);
		}
	}

	public static WeakHashMap<Plan, DragIntent> getDiagramDragIntentByPlan() {
		return diagramDragIntentByPlan;
	}

	public static void backupRootInstanceBuilderState(RootInstanceBuilder rootInstanceBuilder) {
		Object rootInitializer = rootInstanceBuilder.getRootInstantiationNode();
		Object rootInitializerBackup;
		if (rootInitializer != null) {
			rootInitializerBackup = InstantiationUtils.cloneInitializer(rootInitializer);
		} else {
			rootInitializerBackup = null;
		}
		rootInitializerBackupByBuilder.put(rootInstanceBuilder, rootInitializerBackup);
	}

	public static Runnable getRootInstanceBuilderStateRestorationJob(RootInstanceBuilder rootInstanceBuilder) {
		if (!rootInitializerBackupByBuilder.containsKey(rootInstanceBuilder)) {
			throw new UnexpectedError();
		}
		final Object rootInitializerBackup = rootInitializerBackupByBuilder.get(rootInstanceBuilder);
		return new Runnable() {
			@Override
			public void run() {
				rootInstanceBuilder.setRootInstantiationNode((rootInitializerBackup == null) ? null
						: InstantiationUtils.cloneInitializer(rootInitializerBackup));
			}
		};
	}

	protected String selectSubCustomizationsSwitch(Class<?> objectClass) {
		if (objectClass == RootInstanceBuilder.class) {
			return GLOBAL_EXCLUSIVE_CUSTOMIZATIONS;
		}
		if (objectClass == Reference.class) {
			return GLOBAL_EXCLUSIVE_CUSTOMIZATIONS;
		}
		if (Structure.class.isAssignableFrom(objectClass)) {
			return GLOBAL_EXCLUSIVE_CUSTOMIZATIONS;
		}
		if (objectClass.getEnclosingClass() != null) {
			return selectSubCustomizationsSwitch(objectClass.getEnclosingClass());
		}
		if (Operation.class.isAssignableFrom(objectClass)) {
			return objectClass.getName();
		} else if (OperationBuilder.class.isAssignableFrom(objectClass)) {
			return MiscUtils.inferOperationClass(objectClass).getName();
		} else if (Activator.class.isAssignableFrom(objectClass)) {
			return objectClass.getName();
		} else if (Resource.class.isAssignableFrom(objectClass)) {
			return objectClass.getName();
		} else if (CompositeStep.class.isAssignableFrom(objectClass)) {
			return objectClass.getName();
		} else {
			return null;
		}
	}

	protected Class<?> getMainCustomizedClass(String customizationsIdentifier) {
		Class<?> result = MiscUtils.getJESBClass(customizationsIdentifier);
		if (Operation.class.isAssignableFrom(result)) {
			result = MiscUtils.findOperationBuilderClass(result.asSubclass(Operation.class));
		}
		return result;
	}

	@Override
	public String getInfoCustomizationsOutputFilePath(String customizationsIdentifier) {
		String customizationsDirectoryPath = System.getProperty(GUI_CUSTOMIZATIONS_RESOURCE_DIRECTORY_PROPERTY_KEY);
		if (customizationsDirectoryPath != null) {
			return customizationsDirectoryPath + "/" + getInfoCustomizationsResourceName(customizationsIdentifier);
		} else {
			return null;
		}
	}

	protected String getInfoCustomizationsResourceName(String customizationsIdentifier) {
		return ((customizationsIdentifier != GLOBAL_EXCLUSIVE_CUSTOMIZATIONS) ? (customizationsIdentifier + "-") : "")
				+ GUI_MAIN_CUSTOMIZATIONS_RESOURCE_NAME;
	}

	@Override
	protected SubSwingCustomizer createSubCustomizer(String customizationsIdentifier) {
		SubSwingCustomizer result = new JESBSubSwingCustomizer(customizationsIdentifier);
		String customizationsFilePath = getInfoCustomizationsOutputFilePath(customizationsIdentifier);
		if (customizationsFilePath != null) {
			result.setInfoCustomizationsOutputFilePath(customizationsFilePath);
		} else {
			try {
				InputStream stream = result.getClassPathResourceLoader()
						.getResourceAsStream(getInfoCustomizationsResourceName(customizationsIdentifier));
				if (stream != null) {
					result.getInfoCustomizations().loadFromStream(stream, null);
				}
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
		}
		return result;
	}

	@Override
	protected SubCustomizedUI createSubCustomizedUI(String switchIdentifier) {
		return new JESBSubCustomizedUI(switchIdentifier);
	}

	private void setFocusTrackingDisabled(boolean b) {
		focusTrackingDisabled = b;
		if (!b) {
			while (gainedFocusWhileTrackingDisabled.size() > 0) {
				Pair<ITypeInfo, Object> pair = gainedFocusWhileTrackingDisabled.remove(0);
				handleFocusEvent(pair.getFirst(), pair.getSecond(), true);
			}
			while (lostFocusWhileTrackingDisabled.size() > 0) {
				Pair<ITypeInfo, Object> pair = lostFocusWhileTrackingDisabled.remove(0);
				handleFocusEvent(pair.getFirst(), pair.getSecond(), false);
			}
		}
	}

	private boolean handleFocusEvent(ITypeInfo type, Object object, boolean focusGainedOrLost) {
		if (focusTrackingDisabled) {
			Pair<ITypeInfo, Object> pair = new Pair<ITypeInfo, Object>(type, object);
			if (focusGainedOrLost) {
				if (!lostFocusWhileTrackingDisabled.remove(pair)) {
					gainedFocusWhileTrackingDisabled.add(pair);
				}
			} else {
				if (!gainedFocusWhileTrackingDisabled.remove(pair)) {
					lostFocusWhileTrackingDisabled.add(pair);
				}
			}
			return false;
		}
		if (focusGainedOrLost) {
			if (object instanceof Asset) {
				stackOfCurrentAssets.push((Asset) object);
				return true;
			} else if (object instanceof PlanElement) {
				stackOfCurrentPlanElements.push((PlanElement) object);
				return true;
			} else if (object instanceof Activator) {
				stackOfCurrentActivators.push((Activator) object);
				return true;
			} else if (object instanceof Facade) {
				stackOfCurrentInstantiationFacades.push((Facade) object);
				return true;
			}
		} else {
			Consumer<Deque<?>> handler = new Consumer<Deque<?>>() {
				@Override
				public void accept(Deque<?> stack) {
					Object peeked;
					if ((peeked = stack.peek()) != object) {
						if (JESB.isDebugModeActive()) {
							Log.get().error(new UnexpectedError("The user interface may become unstable because "
									+ object + " was abnormally hidden before " + peeked));
						}
					}
					if (!stack.remove(object)) {
						throw new UnexpectedError();
					}
				}
			};
			if (object instanceof Asset) {
				handler.accept(stackOfCurrentAssets);
				return true;
			} else if (object instanceof PlanElement) {
				handler.accept(stackOfCurrentPlanElements);
				return true;
			} else if (object instanceof Activator) {
				handler.accept(stackOfCurrentActivators);
				return true;
			} else if (object instanceof Facade) {
				handler.accept(stackOfCurrentInstantiationFacades);
				return true;
			}
		}
		return false;
	}

	private static boolean isConstantInstanceBuilder(RootInstanceBuilderFacade rootInstanceBuilderFacade) {
		Accessor<String> rootInstanceDynamicTypeNameAccessor = rootInstanceBuilderFacade.getUnderlying()
				.getRootInstanceDynamicTypeNameAccessor();
		if (rootInstanceDynamicTypeNameAccessor != null) {
			if (rootInstanceDynamicTypeNameAccessor.getClass().getName()
					.startsWith(Debugger.PlanActivation.class.getName())) {
				return true;
			}
		}
		return false;
	}

	private class JESBSubSwingCustomizer extends SubSwingCustomizer {

		public JESBSubSwingCustomizer(String switchIdentifier) {
			super(switchIdentifier);
		}

		@Override
		public List<IFieldControlPlugin> getFieldControlPlugins() {
			List<IFieldControlPlugin> result = new ArrayList<IFieldControlPlugin>(super.getFieldControlPlugins());
			result.add(new JESDDatePickerPlugin());
			result.add(new JESDDateTimePickerPlugin());
			return result;
		}

		@Override
		protected CustomizationController createCustomizationController() {
			return new CustomizationController(this) {

				@Override
				protected void recustomizeAllForms() {
					setFocusTrackingDisabled(true);
					try {
						super.recustomizeAllForms();
					} finally {
						setFocusTrackingDisabled(false);
					}
				}
			};
		}

		@Override
		protected CustomizationTools createCustomizationTools() {
			return new CustomizationTools(this) {

				@Override
				protected CustomizationToolsUI createToolsUI() {
					return new CustomizationToolsUI(super.createToolsUI().getInfoCustomizations(), swingCustomizer) {

						@Override
						public ITypeInfo getTypeInfo(ITypeInfoSource typeSource) {
							typeSource = adaptTypeInfoSource(typeSource);
							return super.getTypeInfo(typeSource);
						}

						@Override
						public ITypeInfoSource getTypeInfoSource(Object object) {
							ITypeInfoSource result = super.getTypeInfoSource(object);
							result = adaptTypeInfoSource(result);
							return result;
						}

						ITypeInfoSource adaptTypeInfoSource(ITypeInfoSource typeSource) {
							if (typeSource instanceof JavaTypeInfoSource) {
								JavaTypeInfoSource javaTypeInfoSource = (JavaTypeInfoSource) typeSource;
								if (CustomizationController.class.isAssignableFrom(javaTypeInfoSource.getJavaType())) {
									if (CustomizationController.class != javaTypeInfoSource.getJavaType()) {
										typeSource = new JavaTypeInfoSource(CustomizationController.class,
												javaTypeInfoSource.getSpecificitiesIdentifier());
									}
								}
							}
							return typeSource;
						}
					};
				}
			};
		}

		@Override
		public CustomizingForm subCreateForm(final Object object, IInfoFilter infoFilter) {
			if (object instanceof xy.reflect.ui.control.plugin.AbstractSimpleCustomizableFieldControlPlugin.AbstractConfiguration) {
				return new CustomizingForm(getCustomizationTools().getToolsRenderer(), object, infoFilter);
			}
			return new CustomizingForm(this, object, infoFilter) {

				private static final long serialVersionUID = 1L;

				{
					if (object instanceof FacadeOutline) {
						List<Form> rootInstanceBuilderFacadeForms = SwingRendererUtils.findDescendantFormsOfType(this,
								RootInstanceBuilderFacade.class.getName(), swingRenderer);
						InstanceBuilderInitializerTreeControl initializerTreeControl = (InstanceBuilderInitializerTreeControl) rootInstanceBuilderFacadeForms
								.get(1).getFieldControlPlaceHolder("children").getFieldControl();
						initializerTreeControl.visitItems(new ListControl.IItemsVisitor() {
							@Override
							public VisitStatus visitItem(BufferedItemPosition itemPosition) {
								Facade targetFacade = ((FacadeOutline) object).getFacade();
								Facade currentFacade = (Facade) itemPosition.getItem();
								if (targetFacade.equals(currentFacade)) {
									initializerTreeControl.setSingleSelection(itemPosition);
									return VisitStatus.TREE_VISIT_INTERRUPTED;
								}
								if (!Facade.getAncestors(targetFacade).contains(currentFacade)) {
									return VisitStatus.SUBTREE_VISIT_INTERRUPTED;
								}
								return VisitStatus.VISIT_NOT_INTERRUPTED;
							}
						});
					}
				}

				@Override
				public void refresh(boolean refreshStructure) {
					handleFocusEvent(filteredObjectType, object, true);
					try {
						super.refresh(refreshStructure);
					} finally {
						handleFocusEvent(filteredObjectType, object, false);
					}
				}

				@Override
				protected CustomizingFieldControlPlaceHolder createFieldControlPlaceHolder(IFieldInfo field) {
					return new CustomizingFieldControlPlaceHolder(this, field) {

						private static final long serialVersionUID = 1L;

						FadingPanel transparentPanel = new FadingPanel();
						Object lastValue;
						boolean fadingEnabled = false;

						boolean mayFade() {
							if (!Preferences.INSTANCE.isFadingTransitioningEnabled()) {
								return false;
							}
							if (getObject() instanceof PrecomputedTypeInstanceWrapper) {
								ITypeInfo precomputedType = ((PrecomputedTypeInstanceWrapper) getObject())
										.getPrecomputedType();
								if (precomputedType instanceof EncapsulatedObjectFactory.TypeInfo) {
									EncapsulatedObjectFactory factory = ((EncapsulatedObjectFactory.TypeInfo) precomputedType)
											.getFactory();
									if (factory instanceof AbstractEditorBuilder.EditorEncapsulation) {
										AbstractEditorFormBuilder builder = ((AbstractEditorFormBuilder.EditorEncapsulation) factory)
												.getBuilder();
										if (builder.getClass().getEnclosingClass() == ListControl.class) {
											return true;
										}
									}
								}
							}
							return false;
						}

						boolean prepareToFade() {
							if (fadingEnabled != mayFade()) {
								if (fieldControl != null) {
									destroyFieldControl();
								}
								fadingEnabled = mayFade();
								super.refreshUI(false);
							}
							if (!fadingEnabled) {
								return false;
							}
							Object newValue = field.getValue(getObject());
							if (MiscUtils.equalsOrBothNull(lastValue, newValue)) {
								return false;
							}
							lastValue = newValue;
							if (!isDisplayable()) {
								return false;
							}
							return true;
						}

						@Override
						protected void layoutFieldControl() {
							super.layoutFieldControl();
							if (fadingEnabled) {
								remove(fieldControl);
								add(transparentPanel, BorderLayout.CENTER);
								transparentPanel.add(fieldControl, BorderLayout.CENTER);
							}
						}

						@Override
						protected void destroyFieldControl() {
							if (fadingEnabled) {
								transparentPanel.remove(fieldControl);
								remove(transparentPanel);
								add(fieldControl, BorderLayout.CENTER);
							}
							super.destroyFieldControl();
						}

						@Override
						public void refreshUI(boolean refreshStructure) {
							boolean mustFade = isVisible() && prepareToFade();
							if (mustFade) {
								transparentPanel.fade(+1, 0.0);
							}
							super.refreshUI(refreshStructure);
							if (mustFade) {
								SwingRendererUtils.handleComponentSizeChange(this);
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										transparentPanel.fade(-1, 1.0);
									}
								});
							}
							if (object instanceof InstanceBuilderFacade) {
								if (field.getName().equals("constructorGroup")) {
									setVisible(((InstanceBuilderFacade) object).getConstructorSignatureOptions()
											.size() > 1);
								}
								if (field.getName().equals("typeGroup")) {
									setVisible(((InstanceBuilderFacade) object).getUnderlying()
											.getDynamicTypeNameAccessor() == null);
								}
							}
						}

						@Override
						public Component createFieldControl() {
							if (field.getType().getName().equals(PlanDiagram.Source.class.getName())) {
								return new PlanDiagram(swingRenderer, this);
							}
							if (field.getType().getName().equals(PlanDiagramPalette.Source.class.getName())) {
								return new PlanDiagramPalette(swingRenderer, this);
							}
							if (field.getType().getName().equals(DebugPlanDiagram.Source.class.getName())) {
								return new DebugPlanDiagram(swingRenderer, this);
							}
							if (field.getName().equals("children") && (field.getType() instanceof IListTypeInfo)
									&& (((IListTypeInfo) field.getType()).getItemType() != null)
									&& ((IListTypeInfo) field.getType()).getItemType().getName()
											.equals(Facade.class.getName())) {
								return new InstanceBuilderInitializerTreeControl(swingRenderer, this);
							}
							if (field.getName().equals("data") && (field.getType() instanceof IListTypeInfo)
									&& (((IListTypeInfo) field.getType()).getItemType() != null)
									&& ((IListTypeInfo) field.getType()).getItemType().getName()
											.equals(PathNode.class.getName())) {
								return new InstanceBuilderVariableTreeControl(swingRenderer, this);
							}
							if (field.getName().equals("rootPathNodes") && (field.getType() instanceof IListTypeInfo)
									&& (((IListTypeInfo) field.getType()).getItemType() != null)
									&& ((IListTypeInfo) field.getType()).getItemType().getName()
											.equals(PathNode.class.getName())) {
								return new FunctionEditorVariableTreeControl(swingRenderer, this);
							}
							if (field.getName().equals("functionBody")
									&& (field.getType().getName().equals(String.class.getName()))) {
								return new EditorPlugin().new EditorControl(swingRenderer, this) {

									private static final long serialVersionUID = 1L;

									@Override
									protected JTextComponent createTextComponent() {
										JEditorPane result = (JEditorPane) super.createTextComponent();
										JavaSyntaxKit editorKit = new JavaSyntaxKit();
										editorKit.getConfig().put(JavaSyntaxKit.CONFIG_ENABLE_WORD_WRAP,
												Boolean.TRUE.toString());
										result.setEditorKit(editorKit);
										result.setTransferHandler(
												new FunctionEditorVariableTreeControl.PathImportTransferHandler());
										result.setDropMode(DropMode.INSERT);
										return result;
									}

								};
							}
							if (field.getName().equals("facadeOutlineChildren")
									&& (field.getType() instanceof IListTypeInfo)
									&& (((IListTypeInfo) field.getType()).getItemType() != null)
									&& ((IListTypeInfo) field.getType()).getItemType().getName()
											.equals(FacadeOutline.class.getName())) {
								return new InstanceBuilderOutlineTreeControl(swingRenderer, this);
							}
							if (field.getType().getName().equals(MappingsControl.Source.class.getName())) {
								return new MappingsControl(swingRenderer, this);
							}
							if (filteredObjectType.getName().equals(
									"CapsuleFieldType [context=EncapsulationContext [objectType=CapsuleFieldType [context=EncapsulationContext [objectType="
											+ FieldInitializerFacade.class.getName()
											+ "], fieldName=valueGroup]], fieldName=valueBox]")) {
								if (field.getName().equals("fieldValue")) {
									return new NullableControl(this.swingRenderer, this) {

										private static final long serialVersionUID = 1L;

										@Override
										protected Object getNewValue() {
											FieldInitializerFacade facade = (FieldInitializerFacade) ((MembersCapsuleFieldInfo.Value) ((PrecomputedTypeInstanceWrapper) ((MembersCapsuleFieldInfo.Value) ((PrecomputedTypeInstanceWrapper) getObject())
													.getInstance()).getObject()).getInstance()).getObject();
											if (((facade.getFieldValueMode() == null)
													|| (facade.getFieldValueMode() == ValueMode.PLAIN))
													&& !Object.class.getName().equals(facade.getFieldTypeName())) {
												return facade.createDefaultFieldValue();
											} else {
												return super.getNewValue();
											}
										}

									};
								}
							}
							if (filteredObjectType.getName().equals(
									"CapsuleFieldType [context=EncapsulationContext [objectType=CapsuleFieldType [context=EncapsulationContext [objectType="
											+ ParameterInitializerFacade.class.getName()
											+ "], fieldName=valueGroup]], fieldName=valueBox]")) {
								if (field.getName().equals("parameterValue")) {
									return new NullableControl(this.swingRenderer, this) {

										private static final long serialVersionUID = 1L;

										@Override
										protected Object getNewValue() {
											ParameterInitializerFacade facade = (ParameterInitializerFacade) ((MembersCapsuleFieldInfo.Value) ((PrecomputedTypeInstanceWrapper) ((MembersCapsuleFieldInfo.Value) ((PrecomputedTypeInstanceWrapper) getObject())
													.getInstance()).getObject()).getInstance()).getObject();
											if ((facade.getParameterValueMode() == ValueMode.PLAIN)
													&& !Object.class.getName().equals(facade.getParameterTypeName())) {
												return facade.createDefaultParameterValue();
											} else {
												return super.getNewValue();
											}
										}

									};
								}
							}
							if (filteredObjectType.getName().equals(
									"CapsuleFieldType [context=EncapsulationContext [objectType=CapsuleFieldType [context=EncapsulationContext [objectType="
											+ ListItemInitializerFacade.class.getName()
											+ "], fieldName=valueGroup]], fieldName=valueBox]")) {
								if (field.getName().equals("itemValue")) {
									return new NullableControl(this.swingRenderer, this) {

										private static final long serialVersionUID = 1L;

										@Override
										protected Object getNewValue() {
											ListItemInitializerFacade facade = (ListItemInitializerFacade) ((MembersCapsuleFieldInfo.Value) ((PrecomputedTypeInstanceWrapper) ((MembersCapsuleFieldInfo.Value) ((PrecomputedTypeInstanceWrapper) getObject())
													.getInstance()).getObject()).getInstance()).getObject();
											if (((facade.getItemValueMode() == null)
													|| (facade.getItemValueMode() == ValueMode.PLAIN))
													&& !Object.class.getName().equals(facade.getItemTypeName())) {
												return facade.createDefaultItemValue();
											} else {
												return super.getNewValue();
											}
										}

									};
								}
							}
							if (filteredObjectType.getName().equals(CompilationError.class.getName())) {
								if (field.getName().equals("sourceCode")) {
									final EditorPlugin editorPlugin = new EditorPlugin();
									final EditorPlugin.EditorConfiguration pluginConfiguration = new EditorPlugin.EditorConfiguration();
									{
										pluginConfiguration.syntaxImplementationClassName = JavaSyntaxKit.class
												.getName();
										pluginConfiguration.height = new StyledTextPlugin.StyledTextConfiguration.ControlDimensionSpecification();
										pluginConfiguration.height.value = 350;
									}
									EditorPlugin.EditorControl result = editorPlugin.new EditorControl(swingRenderer,
											new FieldControlInputProxy(this) {
												@Override
												public IFieldControlData getControlData() {
													return new FieldControlDataProxy(super.getControlData()) {
														@Override
														public ITypeInfo getType() {
															return new InfoProxyFactory() {
																@Override
																protected Map<String, Object> getSpecificProperties(
																		ITypeInfo type) {
																	Map<String, Object> result = new HashMap<String, Object>(
																			super.getSpecificProperties(type));
																	ReflectionUIUtils.updateFieldControlPluginValues(
																			result, editorPlugin.getIdentifier(),
																			pluginConfiguration);
																	return result;
																}
															}.wrapTypeInfo(super.getType());
														}
													};
												}
											});
									final JEditorPane editorPane = (JEditorPane) result.getTextComponent();
									final CompilationError compilationError = (CompilationError) getObject();
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											int errorStart = (compilationError.getStartPosition() == -1) ? 0
													: compilationError.getStartPosition();
											int errorEnd = (compilationError.getEndPosition() == -1)
													? editorPane.getText().length()
													: compilationError.getEndPosition();
											try {
												editorPane.getHighlighter().addHighlight(errorStart, errorEnd,
														new SquigglePainter(Color.RED));
											} catch (BadLocationException e) {
												throw new UnexpectedError(e);
											}
											editorPane.getCaret().setSelectionVisible(true);
											editorPane.setSelectionStart(errorStart);
											editorPane.setSelectionEnd(errorEnd);
										}
									});
									return result;
								}
							}
							return super.createFieldControl();
						}

					};
				}

				@Override
				protected CustomizingMethodControlPlaceHolder createMethodControlPlaceHolder(IMethodInfo method) {
					final CustomizingForm thisForm = this;
					if (method.getName().equals("insertSelectedPathNodeExpression")) {
						method = new MethodInfoProxy(method) {

							@Override
							public List<IParameterInfo> getParameters() {
								return Collections.emptyList();
							}

							@Override
							public Object invoke(Object object, InvocationData invocationData) {
								Form functionEditorForm = SwingRendererUtils.findAncestorFormOfType(thisForm,
										FunctionEditor.class.getName(), swingRenderer);
								TextControl textControl = (TextControl) SwingRendererUtils
										.findDescendantFieldControlPlaceHolder(functionEditorForm, "functionBody",
												swingRenderer)
										.getFieldControl();
								invocationData.getProvidedParameterValues().put(0,
										textControl.getTextComponent().getSelectionStart());
								invocationData.getProvidedParameterValues().put(1,
										textControl.getTextComponent().getSelectionEnd());
								return super.invoke(object, invocationData);
							}

						};
					}
					if (method.getName().equals("executePlan")) {
						method = new MethodInfoProxy(method) {
							Throwable error;

							@Override
							public Object invoke(final Object object, InvocationData invocationData) {
								try {
									return super.invoke(object, invocationData);
								} catch (Throwable t) {
									error = t;
									throw new RuntimeException(t);
								} finally {
									if (error == null) {
										SwingUtilities.invokeLater(new Runnable() {
											@Override
											public void run() {
												postSelectNewPlanExecutor();
											}
										});
									}
								}
							}

							private void postSelectNewPlanExecutor() {
								Form debuggerForm = SwingRendererUtils.findAncestorFormOfType(thisForm,
										Debugger.class.getName(), swingRenderer);
								ListControl planActivationsControl = (ListControl) SwingRendererUtils
										.findDescendantFieldControlPlaceHolder(debuggerForm, "planActivations",
												swingRenderer)
										.getFieldControl();
								planActivationsControl.refreshUI(false);
								Form currentPlanActivatorForm = SwingRendererUtils.findAncestorFormOfType(thisForm,
										PlanActivation.class.getName(), swingRenderer);
								PlanActivation currentPlanActivator = (PlanActivation) currentPlanActivatorForm
										.getObject();
								BufferedItemPosition planActivatorPosition = planActivationsControl
										.findItemPositionByReference(currentPlanActivator);
								BufferedItemPosition lastPlanExecutorPosition = planActivatorPosition
										.getSubItemPositions()
										.get(planActivatorPosition.getSubItemPositions().size() - 1);
								planActivationsControl.setSingleSelection(lastPlanExecutorPosition);
							}

						};
					}
					return super.createMethodControlPlaceHolder(method);
				}

				@Override
				public void validateForm(ValidationSession session) throws Exception {
					if (object instanceof Asset) {
						session.put(CURRENT_ASSET_KEY, object);
					}
					if (object instanceof PlanElement) {
						session.put(CURRENT_PLAN_ELEMENT_KEY, object);
					}
					if (object instanceof Facade) {
						session.put(CURRENT_INSTANTIATION_FACADE_KEY, object);
					}
					if (object instanceof Activator) {
						session.put(CURRENT_ACTIVATOR_KEY, object);
					}
					if (object instanceof FunctionEditor) {
						TextControl textControl = (TextControl) SwingRendererUtils
								.findDescendantFieldControlPlaceHolder(this, "functionBody", swingRenderer)
								.getFieldControl();
						JTextComponent textComponent = textControl.getTextComponent();
						textComponent.getHighlighter().removeAllHighlights();
						try {
							((FunctionEditor) object).validate();
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
						super.validateForm(session);
					}
				}

			};
		}

		@Override
		public Image getIconImage(ResourcePath path) {
			Image result = super.getIconImage(path);
			if (result == null) {
				return null;
			}
			return SwingRendererUtils.scalePreservingRatio(result, 16, 16, Image.SCALE_SMOOTH);
		}

		@Override
		public void openErrorDetailsDialog(Component activatorComponent, Throwable error) {
			openObjectDialog(activatorComponent, new Exception(null, error), null, null, false);
		}

		@Override
		public ClassLoader getClassPathResourceLoader() {
			return MiscUtils.getJESBResourceLoader();
		}

	}

	private class JESBSubCustomizedUI extends SubCustomizedUI {

		private UpToDate<InfoCustomizations> upToDateSubInfoCustomizations = new UpToDate<InfoCustomizations>() {

			@Override
			protected Object retrieveLastVersionIdentifier() {
				if (getCustomizationsIdentifier() == null) {
					return null;
				}
				return getMainCustomizedClass(getCustomizationsIdentifier());
			}

			@Override
			protected InfoCustomizations obtainLatest(Object versionIdentifier) throws VersionAccessException {
				InfoCustomizations result = new InfoCustomizations();
				Class<?> mainCustomizedClass = (Class<?>) versionIdentifier;
				if (mainCustomizedClass != null) {
					Method uiCustomizationsMethod;
					try {
						uiCustomizationsMethod = mainCustomizedClass.getMethod(GUI.UI_CUSTOMIZATIONS_METHOD_NAME,
								InfoCustomizations.class);
					} catch (NoSuchMethodException e) {
						uiCustomizationsMethod = null;
					}
					if (uiCustomizationsMethod != null) {
						try {
							uiCustomizationsMethod.invoke(null, result);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new UnexpectedError(e);
						}
					}
					getCustomizedTypeInfoCache().clear();
				}
				return result;
			}

		};

		public JESBSubCustomizedUI(String switchIdentifier) {
			super(switchIdentifier);
		}

		@Override
		public Class<?> getReflectedClass(String name) throws ClassNotFoundException {
			try {
				return MiscUtils.getJESBClass(name);
			} catch (PotentialError e) {
				throw new ClassNotFoundException(name, e);
			}
		}

		@Override
		protected IInfoProxyFactory getSubInfoCustomizationsFactory() {
			InfoProxyFactoryChain result = new InfoProxyFactoryChain();
			result.accessFactories().add(new InfoCustomizationsFactory(this) {

				@Override
				public String getIdentifier() {
					return "MethodBasedSubInfoCustomizationsFactory [of=" + customizationsIdentifier + "]";
				}

				@Override
				protected IInfoProxyFactory getInfoCustomizationsSetupFactory() {
					return IInfoProxyFactory.NULL_INFO_PROXY_FACTORY;
				}

				@Override
				public InfoCustomizations accessInfoCustomizations() {
					try {
						return upToDateSubInfoCustomizations.get();
					} catch (VersionAccessException e) {
						throw new UnexpectedError(e);
					}
				}
			});
			result.accessFactories().add(super.getSubInfoCustomizationsFactory());
			return result;
		}

		@Override
		protected IInfoProxyFactory createBeforeInfoCustomizationsFactory() {
			return new InfoProxyFactory() {

				boolean isResourceNoteField(IFieldInfo field, ITypeInfo objectType) {
					Class<?> objectClass;
					try {
						objectClass = MiscUtils.getJESBClass(objectType.getName());
					} catch (Exception e) {
						objectClass = null;
					}
					if ((objectClass != null) && Resource.class.isAssignableFrom(objectClass)) {
						if (field.getName().equals("note")) {
							return true;
						}
					}
					return false;
				}

				@Override
				protected InfoCategory getCategory(IFieldInfo field, ITypeInfo objectType) {
					if (isResourceNoteField(field, objectType)) {
						return new InfoCategory("Note", 0, null);
					}
					return super.getCategory(field, objectType);
				}

				@Override
				protected String getCaption(IFieldInfo field, ITypeInfo objectType) {
					if (isResourceNoteField(field, objectType)) {
						return "";
					}
					return super.getCaption(field, objectType);
				}

				@Override
				protected double getDisplayAreaVerticalWeight(IFieldInfo field, ITypeInfo objectType) {
					if (isResourceNoteField(field, objectType)) {
						return 1.0;
					}
					return super.getDisplayAreaVerticalWeight(field, objectType);
				}

				@Override
				protected boolean isDisplayAreaVerticallyFilled(IFieldInfo field, ITypeInfo objectType) {
					if (isResourceNoteField(field, objectType)) {
						return true;
					}
					return super.isDisplayAreaVerticallyFilled(field, objectType);
				}

				@Override
				protected void setValue(IFieldInfo field, Object object, Object value, ITypeInfo objectType) {
					if (object == Preferences.INSTANCE) {
						if (field.getName().equals("theme")) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									GUI.INSTANCE.openInformationDialog(null,
											"Restart the application to apply the change.", null);
								}
							});
						}
					}
					super.setValue(field, object, value, objectType);
				}

				@Override
				protected CategoriesStyle getCategoriesStyle(ITypeInfo type) {
					return CategoriesStyle.STANDARD;
				}

				@Override
				protected boolean isFormControlEmbedded(IFieldInfo field, ITypeInfo objectType) {
					Class<?> fieldClass;
					try {
						fieldClass = MiscUtils.getJESBClass(field.getType().getName());
					} catch (Exception e) {
						fieldClass = null;
					}
					if ((fieldClass != null) && OperationStructureBuilder.class.isAssignableFrom(fieldClass)) {
						return true;
					}
					return super.isFormControlEmbedded(field, objectType);
				}

				@Override
				protected boolean canCopy(ITypeInfo type, Object object) {
					Class<?> objectClass;
					try {
						objectClass = MiscUtils.getJESBClass(type.getName());
					} catch (Exception e) {
						objectClass = null;
					}
					if ((objectClass != null) && Asset.class.isAssignableFrom(objectClass)) {
						return true;
					}
					return super.canCopy(type, object);
				}

				@Override
				protected Object copy(ITypeInfo type, Object object) {
					Class<?> objectClass;
					try {
						objectClass = MiscUtils.getJESBClass(type.getName());
					} catch (Exception e) {
						objectClass = null;
					}
					if ((objectClass != null) && Asset.class.isAssignableFrom(objectClass)) {
						return MiscUtils.copy(object);
					} else {
						return super.copy(type, object);
					}
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
						return getRootInstanceBuilderStateRestorationJob(
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
				protected List<IDynamicListAction> getDynamicActions(IListTypeInfo listType,
						List<? extends ItemPosition> selection,
						Mapper<ItemPosition, ListModificationFactory> listModificationFactoryAccessor) {
					List<IDynamicListAction> result = new ArrayList<IDynamicListAction>(
							super.getDynamicActions(listType, selection, listModificationFactoryAccessor));
					if (selection.size() > 0) {
						final ItemPosition firstItemPosition = selection.get(0);
						if ("Data".equals(firstItemPosition.getRoot().getContainingListTitle())
								&& selection.stream().allMatch(
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
					if ((listType.getItemType() != null)
							&& listType.getItemType().getName().equals(PlanElement.class.getName())) {
						final ItemPosition singleSelection = (selection.size() == 1) ? selection.get(0) : null;
						final Plan plan = getCurrentPlan(null);
						if ((singleSelection != null) && singleSelection.getItem() instanceof Step) {
							final Step currentStep = (Step) singleSelection.getItem();
							result.add(new AbstractDynamicListAction() {

								Transition newTransition;

								@Override
								public String getName() {
									return "connectTo";
								}

								@Override
								public String getCaption() {
									return "Connect To...";
								}

								@Override
								public DisplayMode getDisplayMode() {
									return DisplayMode.CONTEXT_MENU;
								}

								@Override
								public List<IParameterInfo> getParameters() {
									List<IParameterInfo> result = new ArrayList<IParameterInfo>();
									result.add(new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

										@Override
										public String getName() {
											return "otherStep";
										}

										@Override
										public String getCaption() {
											return "Step";
										}

										@Override
										public ITypeInfo getType() {
											return getTypeInfo(new JavaTypeInfoSource(Step.class, null));
										}

										@Override
										public boolean hasValueOptions(Object object) {
											return true;
										}

										@Override
										public Object[] getValueOptions(Object object) {
											return plan.getSteps().stream().filter(step -> (step != currentStep)
													&& (step.getParent() == currentStep.getParent())).toArray();
										}

									});
									return result;
								}

								@Override
								public Object invoke(Object object, InvocationData invocationData) {
									Step otherStep = (Step) invocationData.getParameterValue(0);
									if (otherStep == null) {
										throw new NullPointerException();
									}
									newTransition = new Transition();
									newTransition.setStartStep(currentStep);
									newTransition.setEndStep(otherStep);
									plan.getTransitions().add(newTransition);
									return null;
								}

								@Override
								public List<ItemPosition> getPostSelection() {
									return null;
								}

								@Override
								public Runnable getNextInvocationUndoJob(Object object, InvocationData invocationData) {
									return new Runnable() {
										@Override
										public void run() {
											plan.getTransitions().remove(newTransition);
										}
									};
								}
							});
						}
						if ((singleSelection != null) && singleSelection.getItem() instanceof Transition) {
							Transition currentTransition = (Transition) singleSelection.getItem();
							result.add(getTransitionTransferAction(currentTransition, true, plan));
							result.add(getTransitionTransferAction(currentTransition, false, plan));
						}
					}
					return result;
				}

				IDynamicListAction getTransitionTransferAction(Transition transition, boolean startElseEnd, Plan plan) {
					Step oldStep = startElseEnd ? transition.getStartStep() : transition.getEndStep();
					Step oppositeStep = startElseEnd ? transition.getEndStep() : transition.getStartStep();
					return new AbstractDynamicListAction() {

						@Override
						public String getName() {
							return startElseEnd ? "changeStartStep" : "changeEndStep";
						}

						@Override
						public String getCaption() {
							return startElseEnd ? "Transition Start..." : "Transition End...";
						}

						@Override
						public DisplayMode getDisplayMode() {
							return DisplayMode.CONTEXT_MENU;
						}

						@Override
						public List<IParameterInfo> getParameters() {
							List<IParameterInfo> result = new ArrayList<IParameterInfo>();
							result.add(new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

								@Override
								public String getName() {
									return "step";
								}

								@Override
								public String getCaption() {
									return "Step";
								}

								@Override
								public ITypeInfo getType() {
									return getTypeInfo(new JavaTypeInfoSource(Step.class, null));
								}

								@Override
								public boolean hasValueOptions(Object object) {
									return true;
								}

								@Override
								public Object[] getValueOptions(Object object) {
									return plan.getSteps().stream().filter(step -> (step != oppositeStep)
											&& (step.getParent() == oppositeStep.getParent())).toArray();
								}

								@Override
								public Object getDefaultValue(Object object) {
									return oldStep;
								}

							});
							return result;
						}

						@Override
						public Object invoke(Object object, InvocationData invocationData) {
							Step newStep = (Step) invocationData.getParameterValue(0);
							if (newStep == oldStep) {
								throw new IllegalArgumentException("No change detected!");
							}
							if (startElseEnd) {
								transition.setStartStep(newStep);
							} else {
								transition.setEndStep(newStep);
							}
							return null;
						}

						@Override
						public List<ItemPosition> getPostSelection() {
							return null;
						}

						@Override
						public Runnable getNextInvocationUndoJob(Object object, InvocationData invocationData) {
							return new Runnable() {
								@Override
								public void run() {
									if (startElseEnd) {
										transition.setStartStep(oldStep);
									} else {
										transition.setEndStep(oldStep);
									}
								}
							};
						}
					};
				}

				@Override
				protected String toString(ITypeInfo type, Object object) {
					if (object instanceof OperationMetadata) {
						return ((OperationMetadata<?>) object).getOperationTypeName();
					}
					if (object instanceof Throwable) {
						return object.toString();
					}
					return super.toString(type, object);
				}

				@Override
				protected void onFormVisibilityChange(ITypeInfo type, Object object, boolean visible) {
					if (!handleFocusEvent(type, object, visible)) {
						super.onFormVisibilityChange(type, object, visible);
					}
					if (object instanceof Session) {
						Session session = (Session) object;
						if (session.isActive() != visible) {
							if (visible) {
								session.open();
							} else {
								try {
									session.close();
								} catch (Exception e) {
									throw new PotentialError(e);
								}
							}
						}
					}
					if (object instanceof PluginBuilder) {
						if (!visible) {
							PluginBuilder pluginBuilder = (PluginBuilder) object;
							if (pluginBuilder.isTestingPrepared()) {
								pluginBuilder.unprepareTesting();
							}
						}
					}
				}

				@Override
				protected List<IMethodInfo> getConstructors(ITypeInfo type) {
					Class<?> objectClass;
					try {
						objectClass = MiscUtils.getJESBClass(type.getName());
					} catch (Exception e) {
						objectClass = null;
					}
					if (type.getName().equals(Solution.Singleton.class.getName())) {
						return Collections.singletonList(new AbstractConstructorInfo() {

							@Override
							public Object invoke(Object object, InvocationData invocationData) {
								return new Solution();
							}

							@Override
							public ITypeInfo getReturnValueType() {
								return type;
							}
						});
					}
					if ((objectClass != null) && Asset.class.isAssignableFrom(objectClass)) {
						return super.getConstructors(type).stream()
								.filter(constructor -> constructor.getParameters().size() > 0)
								.collect(Collectors.toList());
					}
					return super.getConstructors(type);
				}

				@Override
				protected List<IFieldInfo> getFields(ITypeInfo type) {
					Class<?> objectClass;
					try {
						objectClass = MiscUtils.getJESBClass(type.getName());
					} catch (Exception e) {
						objectClass = null;
					}
					List<IFieldInfo> baseResult = super.getFields(type);
					for (int i = 0; i < baseResult.size(); i++) {
						if (VariantCustomizations.isVariantField(baseResult.get(i))) {
							baseResult.set(i, VariantCustomizations.adaptVariantField(JESBSubCustomizedUI.this,
									baseResult.get(i), type));
						}
					}
					if ((objectClass != null) && Solution.class.isAssignableFrom(objectClass)) {
						List<IFieldInfo> result = new ArrayList<IFieldInfo>(baseResult);
						result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {

							@Override
							public String getName() {
								return "preferences";
							}

							@Override
							public String getCaption() {
								return ReflectionUIUtils.identifierToCaption(getName());
							}

							@Override
							public Object getValue(Object object) {
								return Preferences.INSTANCE;
							}

							@Override
							public void setValue(Object object, Object value) {
								Preferences.INSTANCE.persist();
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(Preferences.class,
										new SpecificitiesIdentifier(Solution.Singleton.class.getName(), getName())));
							}

							@Override
							public boolean isTransient() {
								return true;
							}

							@Override
							public boolean isGetOnly() {
								return false;
							}

						});
						result.add(new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {

							@Override
							public String getName() {
								return "sidePaneValueName";
							}

							@Override
							public String getCaption() {
								return ReflectionUIUtils.identifierToCaption(getName());
							}

							@Override
							public Object getValue(Object object) {
								return sidePaneValueName;
							}

							@Override
							public void setValue(Object object, Object value) {
								sidePaneValueName = (SidePaneValueName) value;
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(SidePaneValueName.class,
										new SpecificitiesIdentifier(Solution.Singleton.class.getName(), getName())));
							}

							@Override
							public boolean isTransient() {
								return true;
							}

							@Override
							public boolean isGetOnly() {
								return false;
							}

						});
						return result;
					} else if ((objectClass != null) && Throwable.class.isAssignableFrom(objectClass)) {
						List<IFieldInfo> result = new ArrayList<IFieldInfo>();
						for (IFieldInfo field : baseResult) {
							if (field.getName().equals("cause")) {
								field = new ValueAsListFieldInfo(JESBSubCustomizedUI.this, field, type) {

									@Override
									public String getCaption() {
										return "Cause(s)";
									}

									@Override
									public boolean isRelevant(Object object) {
										return getValue(object) != null;
									}

									@Override
									public double getDisplayAreaVerticalWeight() {
										return 1.0;
									}

									@Override
									public boolean isDisplayAreaVerticallyFilled() {
										return true;
									}

									@Override
									protected IListTypeInfo createListType() {
										return new ListTypeInfo() {
											@Override
											public IListItemDetailsAccessMode getDetailsAccessMode() {
												return new EmbeddedItemDetailsAccessMode();
											}

											@Override
											public List<IDynamicListProperty> getDynamicProperties(
													List<? extends ItemPosition> selection,
													Mapper<ItemPosition, ListModificationFactory> listModificationFactoryAccessor) {
												List<IDynamicListProperty> result = new ArrayList<IDynamicListProperty>(
														super.getDynamicProperties(selection,
																listModificationFactoryAccessor));
												result.add(new AbstractDynamicListProperty() {

													@Override
													public String getName() {
														return "stackTrace";
													}

													@Override
													public String getCaption() {
														return "Show Stack Trace";
													}

													@Override
													public void setValue(Object object, Object value) {
														throw new UnsupportedOperationException();
													}

													@Override
													public boolean isGetOnly() {
														return true;
													}

													@Override
													public boolean isEnabled() {
														return selection.size() == 1;
													}

													@Override
													public Object getValue(Object object) {
														return ((Throwable) selection.get(0).getItem()).getStackTrace();
													}

													@Override
													public ITypeInfo getType() {
														return JESBSubCustomizedUI.this.getTypeInfo(
																new JavaTypeInfoSource(StackTraceElement[].class,
																		null));
													}

													@Override
													public DisplayMode getDisplayMode() {
														return DisplayMode.CONTEXT_MENU;
													}
												});
												return result;
											}

											@Override
											public List<IDynamicListAction> getDynamicActions(
													List<? extends ItemPosition> selection,
													Mapper<ItemPosition, ListModificationFactory> listModificationFactoryAccessor) {
												List<IDynamicListAction> result = new ArrayList<IDynamicListAction>(
														super.getDynamicActions(selection,
																listModificationFactoryAccessor));
												result.add(new DynamicListActionProxy(
														IDynamicListAction.NULL_DYNAMIC_LIST_ACTION) {
													@Override
													public String getSignature() {
														return ReflectionUIUtils.buildMethodSignature(this);
													}

													@Override
													public String getName() {
														return "printStackTrace";
													}

													@Override
													public String getCaption() {
														return "Print Stack Trace";
													}

													@Override
													public ITypeInfo getReturnValueType() {
														return null;
													}

													@Override
													public boolean isEnabled(Object object) {
														return selection.size() == 1;
													}

													@Override
													public DisplayMode getDisplayMode() {
														return DisplayMode.CONTEXT_MENU;
													}

													@Override
													public Object invoke(Object object, InvocationData invocationData) {
														Log.get().error(((Throwable) selection.get(0).getItem()));
														return null;
													}
												});
												return result;
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
													public IInfoFilter getItemDetailsInfoFilter(
															ItemPosition itemPosition) {
														return new InfoFilterProxy(
																super.getItemDetailsInfoFilter(itemPosition)) {

															@Override
															public IFieldInfo apply(IFieldInfo field) {
																if (field.getName().equals("cause")) {
																	field = null;
																}
																return field;
															}

															@Override
															public IMethodInfo apply(IMethodInfo method) {
																return method;
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
						List<IFieldInfo> result = new ArrayList<IFieldInfo>(baseResult);
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
								return getTypeInfo(new JavaTypeInfoSource(PlanDiagram.Source.class,
										new SpecificitiesIdentifier(Plan.class.getName(), getName())));
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
								return getTypeInfo(new JavaTypeInfoSource(PlanDiagramPalette.Source.class,
										new SpecificitiesIdentifier(Plan.class.getName(), getName())));
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
					} else if (type.getName().equals(PlanExecutor.class.getName())
							|| type.getName().equals(PlanExecutor.SubPlanExecutor.class.getName()))

					{
						List<IFieldInfo> result = new ArrayList<IFieldInfo>(baseResult);
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
								return getTypeInfo(new JavaTypeInfoSource(DebugPlanDiagram.Source.class,
										new SpecificitiesIdentifier(type.getName(), getName())));
							}

						});
						return result;
					} else if (type.getName().equals(RootInstanceBuilderFacade.class.getName())) {
						List<IFieldInfo> result = new ArrayList<IFieldInfo>(baseResult);
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
								Plan displayedPlan = getCurrentPlan(null);
								if (displayedPlan == null) {
									return null;
								}
								Step currentStep;
								if (displayedPlan.getOutputBuilder() == ((RootInstanceBuilderFacade) object)
										.getUnderlying()) {
									currentStep = null;
								} else {
									currentStep = getCurrentStep(null);
								}
								return new PathOptionsProvider(
										displayedPlan.getValidationContext(currentStep).getVariableDeclarations())
												.getRootPathNodes();
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(List.class, new Class<?>[] { PathNode.class },
										new SpecificitiesIdentifier(RootInstanceBuilderFacade.class.getName(),
												getName())));
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
								return new MappingsControl.Source((RootInstanceBuilderFacade) object,
										getCurrentPlan(null), getCurrentStep(null));
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(MappingsControl.Source.class,
										new SpecificitiesIdentifier(RootInstanceBuilderFacade.class.getName(),
												getName())));
							}

						});
						return result;
					} else if (type.getName().equals(ListItemReplicationFacade.class.getName())) {
						List<IFieldInfo> result = new ArrayList<IFieldInfo>(baseResult);
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
								Plan displayedPlan = getCurrentPlan(null);
								if (displayedPlan == null) {
									return null;
								}
								RootInstanceBuilderFacade displayedRootInstanceBuilderFacade = getCurrentRootInstanceBuilderFacade(
										null);
								if (displayedRootInstanceBuilderFacade == null) {
									return null;
								}
								Step currentStep;
								if (displayedPlan.getOutputBuilder() == displayedRootInstanceBuilderFacade
										.getUnderlying()) {
									currentStep = null;
								} else {
									currentStep = getCurrentStep(null);
								}
								ITypeInfo variableType = ((ListItemReplicationFacade) object)
										.guessIterationVariableTypeInfo(displayedPlan.getValidationContext(currentStep)
												.getVariableDeclarations());
								if (variableType != null) {
									return variableType.getName();
								} else {
									return Object.class.getName();
								}
							}

							@Override
							public ITypeInfo getType() {
								return getTypeInfo(new JavaTypeInfoSource(String.class, new SpecificitiesIdentifier(
										ListItemReplicationFacade.class.getName(), getName())));
							}

						});
						return result;
					} else {
						return baseResult;
					}
				}

				@Override
				protected List<IMethodInfo> getMethods(ITypeInfo type) {
					Class<?> objectClass;
					try {
						objectClass = MiscUtils.getJESBClass(type.getName());
					} catch (Exception e) {
						objectClass = null;
					}
					if ((objectClass != null) && Solution.class.isAssignableFrom(objectClass)) {
						List<IMethodInfo> result = new ArrayList<IMethodInfo>(super.getMethods(type));
						result.add(new MethodInfoProxy(IMethodInfo.NULL_METHOD_INFO) {

							@Override
							public String getSignature() {
								return ReflectionUIUtils.buildMethodSignature(this);
							}

							@Override
							public String getName() {
								return "isSidePanelValueSelected";
							}

							@Override
							public String getCaption() {
								return ReflectionUIUtils.formatMethodCaption(this, getName(), 0);
							}

							@Override
							public List<IParameterInfo> getParameters() {
								return Collections
										.singletonList(new ParameterInfoProxy(IParameterInfo.NULL_PARAMETER_INFO) {

											@Override
											public String getName() {
												return "sidePaneValueNameParam";
											}

											@Override
											public String getCaption() {
												return ReflectionUIUtils.identifierToCaption(getName());
											}

											@Override
											public ITypeInfo getType() {
												return getTypeInfo(
														new JavaTypeInfoSource(SidePaneValueName.class, null));
											}
										});
							}

							@Override
							public Object invoke(Object object, InvocationData invocationData) {
								SidePaneValueName sidePaneValueNameParam = (SidePaneValueName) invocationData
										.getParameterValue(0);
								return sidePaneValueName == sidePaneValueNameParam;
							}

							@Override
							public ITypeInfo getReturnValueType() {
								return getTypeInfo(new JavaTypeInfoSource(boolean.class, null));
							}

							@Override
							public boolean isReadOnly() {
								return true;
							}
						});
						return result;
					} else if ((objectClass != null) && com.otk.jesb.Function.class.isAssignableFrom(objectClass)) {
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
								Plan displayedPlan = getCurrentPlan(null);
								Step displayedStep = getCurrentStep(null);
								Transition displayedTransition = getCurrentTransition(null);
								RootInstanceBuilderFacade displayedRootInstanceBuilderFacade = getCurrentRootInstanceBuilderFacade(
										null);
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
									return new FunctionEditor(
											(com.otk.jesb.Function) object, null, ((LoopCompositeStep) displayedStep)
													.getLoopEndConditionVariableDeclarations(displayedPlan),
											boolean.class);
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
								return ((LoopOperation.Builder) object).retrieveResultsCollectionConfigurationEntries(
										getCurrentPlan(null), getCurrentStep(null));
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
										getCurrentPlan(null), getCurrentStep(null));
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
						for (OperationMetadata<?> operationMetadata : MiscUtils.getAllOperationMetadatas()) {
							result.add(getTypeInfo(
									new JavaTypeInfoSource(operationMetadata.getOperationBuilderClass(), null)));
						}
						return result;
					} else if (type.getName().equals(Resource.class.getName())) {
						List<ITypeInfo> result = new ArrayList<ITypeInfo>();
						for (ResourceMetadata resourceMetadata : MiscUtils.getAllResourceMetadatas()) {
							result.add(getTypeInfo(new JavaTypeInfoSource(resourceMetadata.getResourceClass(), null)));
						}
						return result;
					} else if (type.getName().equals(Activator.class.getName())) {
						List<ITypeInfo> result = new ArrayList<ITypeInfo>();
						for (ActivatorMetadata activatorMetadata : MiscUtils.getAllActivatorMetadatas()) {
							result.add(
									getTypeInfo(new JavaTypeInfoSource(activatorMetadata.getActivatorClass(), null)));
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
					for (OperationMetadata<?> operationMetadata : MiscUtils.getAllOperationMetadatas()) {
						if (operationMetadata.getOperationBuilderClass().getName().equals(type.getName())) {
							return operationMetadata.getOperationTypeName();
						}
					}
					for (ResourceMetadata resourceMetadata : MiscUtils.getAllResourceMetadatas()) {
						if (resourceMetadata.getResourceClass().getName().equals(type.getName())) {
							return resourceMetadata.getResourceTypeName();
						}
					}
					for (ActivatorMetadata activatorMetadata : MiscUtils.getAllActivatorMetadatas()) {
						if (activatorMetadata.getActivatorClass().getName().equals(type.getName())) {
							return activatorMetadata.getActivatorName();
						}
					}
					return super.getCaption(type);
				}

				@Override
				protected ResourcePath getIconImagePath(ITypeInfo type, Object object) {
					for (OperationMetadata<?> operationMetadata : MiscUtils.getAllOperationMetadatas()) {
						if (operationMetadata.getOperationBuilderClass().getName().equals(type.getName())) {
							return operationMetadata.getOperationIconImagePath();
						}
					}
					for (ResourceMetadata resourceMetadata : MiscUtils.getAllResourceMetadatas()) {
						if (resourceMetadata.getResourceClass().getName().equals(type.getName())) {
							return resourceMetadata.getResourceIconImagePath();
						}
					}
					for (ActivatorMetadata activatorMetadata : MiscUtils.getAllActivatorMetadatas()) {
						if (activatorMetadata.getActivatorClass().getName().equals(type.getName())) {
							return activatorMetadata.getActivatorIconImagePath();
						}
					}
					if (object instanceof Step) {
						return MiscUtils.getIconImagePath((Step) object);
					}
					if (object instanceof StepCrossing) {
						return MiscUtils.getIconImagePath(((StepCrossing) object).getStep());
					}
					if (object instanceof PlanActivation) {
						return ReflectionUIUtils.getIconImagePath(JESBSubCustomizedUI.this,
								((PlanActivation) object).getPlan());
					}
					if (object instanceof PlanExecutor) {
						PlanExecutor executor = (PlanExecutor) object;
						if (executor.isActive()) {
							return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
									GUI.class.getPackage().getName().replace(".", "/") + "/running.png"));
						} else {
							if (executor.getExecutionError() == null) {
								return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
										GUI.class.getPackage().getName().replace(".", "/") + "/success.png"));
							} else {
								return new ResourcePath(ResourcePath.specifyClassPathResourceLocation(
										GUI.class.getPackage().getName().replace(".", "/") + "/failure.png"));
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

				@Override
				protected boolean isHidden(IFieldInfo field, ITypeInfo objectType) {
					Class<?> objectClass;
					try {
						objectClass = MiscUtils.getJESBClass(objectType.getName());
					} catch (Exception e) {
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
					if ((objectClass != null) && Asset.class.isAssignableFrom(objectClass)) {
						if (field.getName().equals("fullName")) {
							return true;
						}
						if (field.getName().equals("fileSystemResourceName")) {
							return true;
						}
					}
					if ((objectClass != null) && Activator.class.isAssignableFrom(objectClass)) {
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
						objectClass = MiscUtils.getJESBClass(objectType.getName());
					} catch (Exception e) {
						objectClass = null;
					}
					if ((objectClass != null) && Throwable.class.isAssignableFrom(objectClass)) {
						if (getTypeInfo(new JavaTypeInfoSource(Throwable.class, null)).getMethods().stream().anyMatch(
								throwableMethod -> method.getSignature().equals(throwableMethod.getSignature()))) {
							return true;
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
					if ((objectClass != null) && OperationStructureBuilder.class.isAssignableFrom(objectClass)) {
						if (method.getSignature()
								.matches(MiscUtils
										.escapeRegex(ReflectionUIUtils.buildMethodSignature("<TYPE>", "build",
												Arrays.asList(Plan.ExecutionContext.class.getName(),
														Plan.ExecutionInspector.class.getName())))
										.replace("<TYPE>", ".*"))) {
							return true;
						}
						if (method.getSignature()
								.equals(ReflectionUIUtils.buildMethodSignature(Class.class.getName(),
										"getOperationResultClass",
										Arrays.asList(Plan.class.getName(), Step.class.getName())))) {
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
					if ((objectClass != null) && ListItemReplicationFacade.class.isAssignableFrom(objectClass)) {
						if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
								Arrays.asList(List.class.getName())))) {
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
					if ((objectClass != null) && ActivatorStructure.class.isAssignableFrom(objectClass)) {
						if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
								Arrays.asList(boolean.class.getName(), Plan.class.getName())))) {
							return true;
						}
					}
					if ((objectClass != null) && Activator.class.isAssignableFrom(objectClass)) {
						if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void",
								"initializeAutomaticTrigger", Arrays.asList(ActivationHandler.class.getName())))) {
							return true;
						}
					}
					if ((objectClass != null) && Activator.class.isAssignableFrom(objectClass)) {
						if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void",
								"finalizeAutomaticTrigger", Arrays.asList()))) {
							return true;
						}
					}
					if ((objectClass != null) && HTTPServer.RequestHandler.class.isAssignableFrom(objectClass)) {
						if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
								Arrays.asList(HTTPServer.class.getName())))) {
							return true;
						}
					}
					if ((objectClass != null) && ResourceStructure.class.isAssignableFrom(objectClass)) {
						if (method.getSignature().equals(ReflectionUIUtils.buildMethodSignature("void", "validate",
								Arrays.asList(boolean.class.getName())))) {
							return true;
						}
					}
					return super.isHidden(method, objectType);
				}

				@Override
				protected boolean isRelevant(IFieldInfo field, Object object, ITypeInfo objectType) {
					Class<?> objectClass;
					try {
						objectClass = MiscUtils.getJESBClass(objectType.getName());
					} catch (Exception e) {
						objectClass = null;
					}
					if ((objectClass != null) && Throwable.class.isAssignableFrom(objectClass)) {
						if (field.getName().equals("message")) {
							return field.getValue(object) != null;
						}
					}
					return super.isRelevant(field, object, objectType);
				}

				@Override
				protected boolean isModificationStackAccessible(ITypeInfo type) {
					try {
						Class<?> objectClass = MiscUtils.getJESBClass(type.getName());
						if (Throwable.class.isAssignableFrom(objectClass)) {
							return false;
						}
					} catch (Exception e) {
					}
					return super.isModificationStackAccessible(type);
				}

				@Override
				protected void validate(ITypeInfo type, Object object, ValidationSession session) throws Exception {
					Class<?> objectClass;
					try {
						objectClass = MiscUtils.getJESBClass(type.getName());
					} catch (Exception e) {
						objectClass = null;
					}
					if ((objectClass != null) && Asset.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, false);
						}
						if (!(object instanceof Folder)) {
							if (Log.isVerbose()) {
								Log.get().information("Validating '" + ((Asset) object).getFullName() + "'...");
							}
						}
						((Asset) object).validate(false);
					} else if ((objectClass != null) && Step.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, true);
						}
						if (Log.isVerbose()) {
							Log.get().information("Validating plan step '" + ((Step) object).getName() + "'...");
						}
						((Step) object).validate(false, getCurrentPlan(session));
					} else if ((objectClass != null) && Transition.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, true);
						}
						if (Log.isVerbose()) {
							Log.get().information(
									"Validating plan transition '" + ((Transition) object).getSummary() + "'...");
						}
						((Transition) object).validate(false, getCurrentPlan(session));
					} else if ((objectClass != null) && Transition.Condition.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, true);
						}
						((Transition.Condition) object).validate(getCurrentPlan(session)
								.getTransitionContextVariableDeclarations(getCurrentTransition(session)));
					} else if ((objectClass != null) && OperationStructureBuilder.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, true);
						}
						((OperationStructureBuilder<?>) object).validate(false, getCurrentPlan(session),
								getCurrentStep(session));
					} else if ((objectClass != null) && Facade.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, true);
						}
						RootInstanceBuilderFacade rootInstanceBuilderFacade = (RootInstanceBuilderFacade) Facade
								.getRoot((Facade) object);
						if (isConstantInstanceBuilder(rootInstanceBuilderFacade)) {
							((Facade) object).validate(false, Collections.emptyList());
							return;
						}
						Step step = getCurrentStep(session);
						Plan plan = getCurrentPlan(session);
						step = (plan.getOutputBuilder() == rootInstanceBuilderFacade.getUnderlying()) ? null : step;
						((Facade) object).validate(false, plan.getValidationContext(step).getVariableDeclarations());
					} else if ((objectClass != null) && ListItemReplicationFacade.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, true);
						}
						RootInstanceBuilderFacade rootInstanceBuilderFacade = (RootInstanceBuilderFacade) Facade
								.getRoot(((ListItemReplicationFacade) object).getListItemInitializerFacade());
						if (isConstantInstanceBuilder(rootInstanceBuilderFacade)) {
							((ListItemReplicationFacade) object).validate(Collections.emptyList());
							return;
						}
						Step step = getCurrentStep(session);
						Plan plan = getCurrentPlan(session);
						step = (plan.getOutputBuilder() == rootInstanceBuilderFacade.getUnderlying()) ? null : step;
						((ListItemReplicationFacade) object)
								.validate(plan.getValidationContext(step).getVariableDeclarations());
					} else if ((objectClass != null) && Structure.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, false);
						}
						((Structure) object).validate(false);
					} else if ((objectClass != null) && Structure.Element.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, false);
						}
						((Structure.Element) object).validate(false);
					} else if ((objectClass != null) && ActivatorStructure.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, true);
						}
						Plan plan = getCurrentPlan(session);
						((ActivatorStructure) object).validate(false, plan);
					} else if ((objectClass != null) && HTTPServer.RequestHandler.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, true);
						}
						HTTPServer server = null;
						{
							Asset currentAsset = getCurrentAsset(session);
							if (currentAsset instanceof HTTPServer) {
								server = (HTTPServer) currentAsset;
							} else {
								Activator activator = getCurrentActivator(session);
								if (activator instanceof HTTPRequestReceiver) {
									server = ((HTTPRequestReceiver) activator).getServerReference().resolve();
								}
							}
						}
						((HTTPServer.RequestHandler) object).validate(server);
					} else if ((objectClass != null) && ResourceStructure.class.isAssignableFrom(objectClass)) {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, false);
						}
						((ResourceStructure) object).validate(false);
					} else {
						if (JESB.isDebugModeActive()) {
							checkValidationErrorMapKeyIsCustomOrNot(object, session, false);
						}
						super.validate(type, object, session);
					}
				}

				private void checkValidationErrorMapKeyIsCustomOrNot(Object object, ValidationSession session,
						boolean custom) {
					if (custom != (object != getValidationErrorRegistry().getValidationErrorMapKey(object, session))) {
						throw new UnexpectedError();
					}
				}

				@Override
				protected IValidationJob getValueAbstractFormValidationJob(IFieldInfo field, Object object,
						ITypeInfo objectType) {
					if (field.getType().getName().equals(RootInstanceBuilder.class.getName())) {
						return (session) -> {
							Object value = field.getValue(object);
							Step step = getCurrentStep(session);
							Plan plan = getCurrentPlan(session);
							RootInstanceBuilderFacade rootInstanceBuilderFacade = ((RootInstanceBuilder) value)
									.getFacade();
							step = (plan.getOutputBuilder() == rootInstanceBuilderFacade.getUnderlying()) ? null : step;
							rootInstanceBuilderFacade.validate(true,
									plan.getValidationContext(step).getVariableDeclarations());
						};
					}
					return super.getValueAbstractFormValidationJob(field, object, objectType);
				}

				@Override
				protected IValidationJob getReturnValueAbstractFormValidationJob(IMethodInfo method, Object object,
						Object returnValue, ITypeInfo objectType) {
					if (returnValue instanceof Activator) {
						return (session) -> {
							Plan plan = getCurrentPlan(session);
							((Activator) returnValue).validate(true, plan);
						};
					}
					return super.getReturnValueAbstractFormValidationJob(method, object, returnValue, objectType);
				}

				@Override
				protected IValidationJob getListItemAbstractFormValidationJob(IListTypeInfo listType,
						ItemPosition itemPosition) {
					Object item = itemPosition.getItem();
					if (item instanceof Asset) {
						return (session) -> {
							if (item instanceof Folder) {
								((Folder) item).validate(false);
							} else {
								if (Log.isVerbose()) {
									Log.get().information("Validating '" + ((Asset) item).getFullName() + "'...");
								}
								((Asset) item).validate(true);
							}
						};
					} else if (item instanceof Facade) {
						return (session) -> {
							RootInstanceBuilderFacade rootInstanceBuilderFacade = (RootInstanceBuilderFacade) Facade
									.getRoot((Facade) item);
							if (isConstantInstanceBuilder(rootInstanceBuilderFacade)) {
								((Facade) item).validate(false, Collections.emptyList());
								return;
							}
							Step step = getCurrentStep(session);
							Plan plan = getCurrentPlan(session);
							step = (plan.getOutputBuilder() == rootInstanceBuilderFacade.getUnderlying()) ? null : step;
							((Facade) item).validate(false, plan.getValidationContext(step).getVariableDeclarations());
						};
					} else if (item instanceof FacadeOutline) {
						return (session) -> {
							RootInstanceBuilderFacade rootInstanceBuilderFacade = (RootInstanceBuilderFacade) Facade
									.getRoot(((FacadeOutline) item).getFacade());
							if (isConstantInstanceBuilder(rootInstanceBuilderFacade)) {
								(((FacadeOutline) item).getFacade()).validate(false, Collections.emptyList());
								return;
							}
							Step step = getCurrentStep(session);
							Plan plan = getCurrentPlan(session);
							step = (plan.getOutputBuilder() == rootInstanceBuilderFacade.getUnderlying()) ? null : step;
							(((FacadeOutline) item).getFacade()).validate(false,
									plan.getValidationContext(step).getVariableDeclarations());
						};
					}
					return super.getListItemAbstractFormValidationJob(listType, itemPosition);
				}

			};
		}

		@Override
		protected ValidationErrorRegistry createValidationErrorRegistry() {
			return new ValidationErrorRegistry() {

				@Override
				public Exception getValidationError(Object object, ValidationSession session) {
					try {
						return super.getValidationError(object, session);
					} catch (Throwable t) {
						return new Exception("Failed to retrieve the validation error", t);
					}
				}

				@Override
				public Object getValidationErrorMapKey(Object object, ValidationSession session) {
					if (object instanceof Step) {
						Plan plan = getCurrentPlan(session);
						if (plan == null) {
							throw new UnexpectedError();
						}
						return Arrays.asList(object, plan);
					} else if (object instanceof Transition) {
						Plan plan = getCurrentPlan(session);
						if (plan == null) {
							throw new UnexpectedError();
						}
						return Arrays.asList(object, plan);
					} else if (object instanceof Transition.Condition) {
						Plan plan = getCurrentPlan(session);
						if (plan == null) {
							throw new UnexpectedError();
						}
						Transition transition = getCurrentTransition(session);
						if (transition == null) {
							throw new UnexpectedError();
						}
						return Arrays.asList(object, plan, transition);
					} else if (object instanceof OperationStructureBuilder) {
						Plan plan = getCurrentPlan(session);
						if (plan == null) {
							throw new UnexpectedError();
						}
						Step step = getCurrentStep(session);
						if (step == null) {
							throw new UnexpectedError();
						}
						return Arrays.asList(object, plan, step);
					} else if (object instanceof Facade) {
						RootInstanceBuilderFacade rootInstanceBuilderFacade = (RootInstanceBuilderFacade) Facade
								.getRoot((Facade) object);
						if (isConstantInstanceBuilder(rootInstanceBuilderFacade)) {
							return Arrays.asList(object, rootInstanceBuilderFacade);
						}
						Plan plan = getCurrentPlan(session);
						if (plan == null) {
							throw new UnexpectedError();
						}
						Step step = (plan.getOutputBuilder() == rootInstanceBuilderFacade.getUnderlying()) ? null
								: getCurrentStep(session);
						return Arrays.asList(object, plan, step, rootInstanceBuilderFacade);
					} else if (object instanceof ListItemReplicationFacade) {
						RootInstanceBuilderFacade rootInstanceBuilderFacade = (RootInstanceBuilderFacade) Facade
								.getRoot(((ListItemReplicationFacade) object).getListItemInitializerFacade());
						if (isConstantInstanceBuilder(rootInstanceBuilderFacade)) {
							return Arrays.asList(object, rootInstanceBuilderFacade);
						}
						Plan plan = getCurrentPlan(session);
						if (plan == null) {
							throw new UnexpectedError();
						}
						Step step = (plan.getOutputBuilder() == rootInstanceBuilderFacade.getUnderlying()) ? null
								: getCurrentStep(session);
						return Arrays.asList(object, plan, step, rootInstanceBuilderFacade);
					} else if (object instanceof ActivatorStructure) {
						Plan plan = getCurrentPlan(session);
						if (plan == null) {
							throw new UnexpectedError();
						}
						return Arrays.asList(object, plan);
					} else if (object instanceof HTTPServer.RequestHandler) {
						Asset asset = getCurrentAsset(session);
						HTTPServer server = null;
						if (asset instanceof HTTPServer) {
							server = (HTTPServer) asset;
						} else {
							Activator activator = getCurrentActivator(session);
							if (activator instanceof HTTPRequestReceiver) {
								server = ((HTTPRequestReceiver) activator).getServerReference().resolve();
							}
						}
						if (server == null) {
							throw new UnexpectedError();
						}
						return Arrays.asList(object, server);
					} else {
						return super.getValidationErrorMapKey(object, session);
					}
				}

			};
		}

		private Asset getCurrentAsset(ValidationSession session) {
			Asset result = (session == null) ? null : (Asset) session.get(CURRENT_ASSET_KEY);
			if (result == null) {
				result = stackOfCurrentAssets.peek();
			}
			return result;
		}

		private PlanElement getCurrentPlanElement(ValidationSession session) {
			PlanElement result = (session == null) ? null : (PlanElement) session.get(CURRENT_PLAN_ELEMENT_KEY);
			if (result == null) {
				result = stackOfCurrentPlanElements.peek();
			}
			return result;
		}

		private Facade getCurrentInstantiationFacade(ValidationSession session) {
			Facade result = (session == null) ? null : (Facade) session.get(CURRENT_INSTANTIATION_FACADE_KEY);
			if (result == null) {
				result = stackOfCurrentInstantiationFacades.peek();
			}
			return result;
		}

		private Activator getCurrentActivator(ValidationSession session) {
			Activator result = (session == null) ? null : (Activator) session.get(CURRENT_ACTIVATOR_KEY);
			if (result == null) {
				result = stackOfCurrentActivators.peek();
			}
			return result;
		}

		private Plan getCurrentPlan(ValidationSession session) {
			Asset current = getCurrentAsset(session);
			if (current instanceof Plan) {
				return (Plan) current;
			}
			Plan result = (Plan) stackOfCurrentAssets.stream().filter(Plan.class::isInstance).findFirst().orElse(null);
			return result;
		}

		private Step getCurrentStep(ValidationSession session) {
			PlanElement current = getCurrentPlanElement(session);
			if (current instanceof Step) {
				return (Step) current;
			}
			Step result = (Step) stackOfCurrentPlanElements.stream().filter(Step.class::isInstance).findFirst()
					.orElse(null);
			return result;
		}

		private Transition getCurrentTransition(ValidationSession session) {
			PlanElement current = getCurrentPlanElement(session);
			if (current instanceof Transition) {
				return (Transition) current;
			}
			Transition result = (Transition) stackOfCurrentPlanElements.stream().filter(Transition.class::isInstance)
					.findFirst().orElse(null);
			return result;
		}

		private RootInstanceBuilderFacade getCurrentRootInstanceBuilderFacade(ValidationSession session) {
			Facade current = getCurrentInstantiationFacade(session);
			if (current instanceof RootInstanceBuilderFacade) {
				return (RootInstanceBuilderFacade) current;
			}
			RootInstanceBuilderFacade result = (RootInstanceBuilderFacade) stackOfCurrentInstantiationFacades.stream()
					.filter(RootInstanceBuilderFacade.class::isInstance).findFirst().orElse(null);
			return result;
		}
	}

	protected static class JESDDatePickerPlugin extends DatePickerPlugin {

		@Override
		protected boolean handles(Class<?> javaType) {
			return javaType == Date.class;
		}

		@Override
		public DatePicker createControl(Object renderer, IFieldControlInput input) {
			return super.createControl(renderer, new FieldControlInputProxy(input) {

				@Override
				public IFieldControlData getControlData() {
					return new FieldControlDataProxy(super.getControlData()) {

						@Override
						public Object getValue() {
							Date value = (Date) super.getValue();
							if (value == null) {
								return null;
							}
							return value.toJavaUtilDate();
						}

						@Override
						public void setValue(Object value) {
							if (value != null) {
								value = Date.fromJavaUtilDate((java.util.Date) value);
							}
							super.setValue(value);
						}

						@Override
						public ITypeInfo getType() {
							ReflectionUI reflectionUI = ((SwingRenderer) renderer).getReflectionUI();
							return reflectionUI.getTypeInfo(new JavaTypeInfoSource(java.util.Date.class, null));
						}
					};
				}
			});
		}

	}

	protected static class JESDDateTimePickerPlugin extends DateTimePickerPlugin {

		@Override
		protected boolean handles(Class<?> javaType) {
			return javaType == DateTime.class;
		}

		@Override
		public DateTimePicker createControl(Object renderer, IFieldControlInput input) {
			return super.createControl(renderer, new FieldControlInputProxy(input) {

				@Override
				public IFieldControlData getControlData() {
					return new FieldControlDataProxy(super.getControlData()) {

						@Override
						public Object getValue() {
							DateTime value = (DateTime) super.getValue();
							if (value == null) {
								return null;
							}
							return value.toJavaUtilDate();
						}

						@Override
						public void setValue(Object value) {
							if (value != null) {
								value = DateTime.fromJavaUtilDate((java.util.Date) value);
							}
							super.setValue(value);
						}

						@Override
						public ITypeInfo getType() {
							ReflectionUI reflectionUI = ((SwingRenderer) renderer).getReflectionUI();
							return reflectionUI.getTypeInfo(new JavaTypeInfoSource(java.util.Date.class, null));
						}
					};
				}
			});
		}

	}

	public static class VariantCustomizations {

		public static boolean isVariantField(IFieldInfo field) {
			return field.getType().getName().equals(Variant.class.getName());
		}

		public static String getAdapterTypeName(String objectTypeName, String variantFieldName) {
			return objectTypeName + "." + variantFieldName.substring(0, 1).toUpperCase() + variantFieldName.substring(1)
					+ "AdapterType";
		}

		public static String getConstantValueFieldName(String variantFieldName) {
			return variantFieldName + "Value";
		}

		public static String getVariableReferenceBoxTypeName(String objectTypeName, String variantFieldName) {
			return objectTypeName + "." + variantFieldName.substring(0, 1).toUpperCase() + variantFieldName.substring(1)
					+ "ReferenceBoxType";
		}

		public static String getVariableReferenceFieldName(String variantFieldName) {
			return variantFieldName + "Reference";
		}

		public static IFieldInfo adaptVariantField(ReflectionUI reflectionUI, IFieldInfo variantField,
				ITypeInfo objectType) {
			return new FieldInfoProxy(variantField) {
				String valueCaption;
				{
					valueCaption = variantField.getCaption();
					if (valueCaption.endsWith(" Variant")) {
						valueCaption = valueCaption.substring(0, valueCaption.length() - " Variant".length());
					}

				}

				@Override
				public String getCaption() {
					return valueCaption;
				}

				@Override
				public boolean isControlValueValiditionEnabled() {
					return true;
				}

				@Override
				public boolean isFormControlEmbedded() {
					return true;
				}

				@Override
				public Object getValue(Object object) {
					return new PrecomputedTypeInstanceWrapper(super.getValue(object), precomputeAdapterType());
				}

				@Override
				public void setValue(Object object, Object value) {
					super.setValue(object, ((PrecomputedTypeInstanceWrapper) value).getInstance());
				}

				@Override
				public ITypeInfo getType() {
					return reflectionUI.getTypeInfo(precomputeAdapterType().getSource());
				}

				ITypeInfo precomputeAdapterType() {
					return new InfoProxyFactory() {

						String adapterTypeName = objectType.getName() + "."
								+ variantField.getName().substring(0, 1).toUpperCase()
								+ variantField.getName().substring(1) + "AdapterWrapperType";

						IFieldInfo variableStatusField = new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {

							@Override
							public String getName() {
								return variantField.getName() + "VariableStatus";
							}

							@Override
							public String getCaption() {
								return "";
							}

							@Override
							public double getDisplayAreaHorizontalWeight() {
								return 0.0;
							}

							@Override
							public boolean isGetOnly() {
								return false;
							}

							@Override
							public Object getValue(Object object) {
								return ((Variant<?>) object).isVariable();
							}

							@Override
							public void setValue(Object object, Object value) {
								((Variant<?>) object).setVariable((boolean) value);
							}

							@Override
							public ITypeInfo getType() {
								return new InfoProxyFactory() {
									ToggleButtonPlugin plugin = new ToggleButtonPlugin();
									ToggleButtonConfiguration pluginConfiguration = new ToggleButtonConfiguration();
									{
										pluginConfiguration.iconImagePath = new ResourcePath(GUI.class,
												"environment.png");
									}

									@Override
									protected Map<String, Object> getSpecificProperties(ITypeInfo type) {
										Map<String, Object> result = new HashMap<String, Object>(
												super.getSpecificProperties(type));
										ReflectionUIUtils.setFieldControlPluginIdentifier(result,
												plugin.getIdentifier());
										ReflectionUIUtils.setFieldControlPluginConfiguration(result,
												plugin.getIdentifier(), pluginConfiguration);
										return result;
									}
								}.wrapTypeInfo(reflectionUI.getTypeInfo(new JavaTypeInfoSource(boolean.class,
										new SpecificitiesIdentifier(adapterTypeName, getName()))));
							}

						};

						IFieldInfo getConstantValueField(String parentTypeName, String caption) {
							return new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {

								@Override
								public String getName() {
									return getConstantValueFieldName(variantField.getName());
								}

								@Override
								public String getCaption() {
									return caption;
								}

								@Override
								public double getDisplayAreaHorizontalWeight() {
									return 1.0;
								}

								@Override
								public boolean isGetOnly() {
									return false;
								}

								@Override
								public boolean isRelevant(Object object) {
									return Boolean.FALSE.equals(variableStatusField.getValue(object));
								}

								@SuppressWarnings({ "rawtypes" })
								@Override
								public Object getValue(Object object) {
									return ((Variant) object).getValue();
								}

								@SuppressWarnings({ "rawtypes" })
								@Override
								public void setValue(Object object, Object value) {
									((Variant) object).setConstantValue(value);
								}

								@Override
								public ITypeInfo getType() {
									return reflectionUI.getTypeInfo(new JavaTypeInfoSource(
											((JavaTypeInfoSource) variantField.getType().getSource())
													.guessGenericTypeParameters(Variant.class, 0),
											new SpecificitiesIdentifier(parentTypeName, getName())));
								}

							};
						}

						IFieldInfo getVariableReferenceField(String parentTypeName, String caption) {
							return new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {

								GenericEnumerationFactory optionsFactory = new GenericEnumerationFactory(reflectionUI,
										new EnvironmentVariableOptionCollector(),
										EnvironmentVariable.class.getName() + "Option", "Environment Variable Option",
										true, false);

								@Override
								public String getName() {
									return getVariableReferenceFieldName(variantField.getName());
								}

								@Override
								public String getCaption() {
									return caption;
								}

								@Override
								public double getDisplayAreaHorizontalWeight() {
									return 1.0;
								}

								@Override
								public boolean isGetOnly() {
									return false;
								}

								@Override
								public boolean isRelevant(Object object) {
									return Boolean.TRUE.equals(variableStatusField.getValue(object));
								}

								@Override
								public Object getValue(Object object) {
									return optionsFactory
											.getItemInstance(((Variant<?>) object).getVariableReferenceExpression());
								}

								@SuppressWarnings("unchecked")
								@Override
								public void setValue(Object object, Object value) {
									((Variant<?>) object).setVariableReferenceExpression(
											(Expression<String>) optionsFactory.getInstanceItem(value));
								}

								@Override
								public ITypeInfo getType() {
									return reflectionUI.getTypeInfo(optionsFactory.getInstanceTypeInfoSource(
											new SpecificitiesIdentifier(parentTypeName, getName())));
								}

							};
						}

						IFieldInfo valuesField = new FieldInfoProxy(IFieldInfo.NULL_FIELD_INFO) {

							@Override
							public String getName() {
								return variantField.getName() + "Values";
							}

							@Override
							public String getCaption() {
								return "";
							}

							@Override
							public double getDisplayAreaHorizontalWeight() {
								return 1.0;
							}

							@Override
							public boolean isFormControlEmbedded() {
								return true;
							}

							@Override
							public Object getValue(Object object) {
								return new PrecomputedTypeInstanceWrapper(object, precomputeValuesType());
							}

							@Override
							public ITypeInfo getType() {
								return reflectionUI.getTypeInfo(precomputeValuesType().getSource());
							}

							ITypeInfo precomputeValuesType() {
								return new InfoProxyFactory() {

									String valuesTypeName = getAdapterTypeName(objectType.getName(),
											variantField.getName());

									private IFieldInfo constantValueField = getConstantValueField(valuesTypeName, "");
									private IFieldInfo variableReferenceBoxField = new FieldInfoProxy(
											IFieldInfo.NULL_FIELD_INFO) {
										@Override
										public String getName() {
											return variantField.getName() + "Reference";
										}

										@Override
										public String getCaption() {
											return "";
										}

										@Override
										public double getDisplayAreaHorizontalWeight() {
											return 1.0;
										}

										@Override
										public boolean isFormControlEmbedded() {
											return true;
										}

										@Override
										public boolean isRelevant(Object object) {
											return Boolean.TRUE.equals(variableStatusField.getValue(object));
										}

										@Override
										public Object getValue(Object object) {
											return new PrecomputedTypeInstanceWrapper(object,
													precomputeVariableReferenceBoxType());
										}

										@Override
										public ITypeInfo getType() {
											return reflectionUI
													.getTypeInfo(precomputeVariableReferenceBoxType().getSource());
										}

										ITypeInfo precomputeVariableReferenceBoxType() {
											return new InfoProxyFactory() {

												String variableReferenceBoxTypeName = getVariableReferenceBoxTypeName(
														objectType.getName(), variantField.getName());

												private IFieldInfo variableReferenceField = getVariableReferenceField(
														variableReferenceBoxTypeName, "");

												@Override
												protected String getName(ITypeInfo type) {
													return variableReferenceBoxTypeName;
												}

												@Override
												protected int getFormSpacing(ITypeInfo type) {
													return 0;
												}

												@Override
												protected ITypeInfoSource getSource(ITypeInfo type) {
													return new PrecomputedTypeInfoSource(
															precomputeVariableReferenceBoxType(),
															new SpecificitiesIdentifier(valuesTypeName,
																	variableReferenceBoxField.getName()));
												}

												@Override
												protected List<IFieldInfo> getFields(ITypeInfo type) {
													return Arrays.asList(variableReferenceField);
												}

												@Override
												protected String toString(ITypeInfo type, Object object) {
													return Objects.toString(object);
												}

												@Override
												public String getIdentifier() {
													return VariantCustomizations.class.getName()
															+ "VariableReferenceBoxFactory [objectType="
															+ objectType.getName() + ", field=" + variantField.getName()
															+ "]";
												}
											}.wrapTypeInfo(ITypeInfo.NULL_BASIC_TYPE_INFO);
										}

									};

									@Override
									protected String getName(ITypeInfo type) {
										return valuesTypeName;
									}

									@Override
									protected int getFormSpacing(ITypeInfo type) {
										return 0;
									}

									@Override
									protected ITypeInfoSource getSource(ITypeInfo type) {
										return new PrecomputedTypeInfoSource(precomputeValuesType(),
												new SpecificitiesIdentifier(adapterTypeName, valuesField.getName()));
									}

									@Override
									protected List<IFieldInfo> getFields(ITypeInfo type) {
										return Arrays.asList(constantValueField, variableReferenceBoxField);
									}

									@Override
									protected String toString(ITypeInfo type, Object object) {
										return Objects.toString(object);
									}

									@Override
									public String getIdentifier() {
										return VariantCustomizations.class.getName() + "ValuesFactory [objectType="
												+ objectType.getName() + ", field=" + variantField.getName() + "]";
									}

								}.wrapTypeInfo(ITypeInfo.NULL_BASIC_TYPE_INFO);
							}

						};

						@Override
						protected String getName(ITypeInfo type) {
							return adapterTypeName;
						}

						@Override
						protected FieldsLayout getFieldsLayout(ITypeInfo type) {
							return FieldsLayout.HORIZONTAL_FLOW;
						}

						@Override
						protected int getFormSpacing(ITypeInfo type) {
							return 5;
						}

						@Override
						protected ITypeInfoSource getSource(ITypeInfo type) {
							return new PrecomputedTypeInfoSource(precomputeAdapterType(),
									new SpecificitiesIdentifier(objectType.getName(), variantField.getName()));
						}

						@Override
						protected List<IFieldInfo> getFields(ITypeInfo type) {
							return Arrays.asList(variableStatusField, valuesField);
						}

						@Override
						protected String toString(ITypeInfo type, Object object) {
							return Objects.toString(object);
						}

						@Override
						protected void validate(ITypeInfo type, Object object, ValidationSession session)
								throws Exception {
							((Variant<?>) object).validate();
						}

						@Override
						public String getIdentifier() {
							return VariantCustomizations.class.getName() + "Factory [objectType=" + objectType.getName()
									+ ", field=" + variantField.getName() + "]";
						}

					}.wrapTypeInfo(ITypeInfo.NULL_BASIC_TYPE_INFO);
				}

			};
		}
	}

	private static class EnvironmentVariableOptionCollector implements Iterable<Expression<String>> {

		@Override
		public Iterator<Expression<String>> iterator() {
			return new Variant<Object>(Object.class).getVariableReferenceExpressionOptions().iterator();
		}

		@Override
		public int hashCode() {
			return 1;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "EnvironmentVariableOptionCollector []";
		}

	}

	public enum SidePaneValueName {
		ENVIRONMENT_SETTINGS
	}

}
