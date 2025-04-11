package com.otk.jesb.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.otk.jesb.Debugger;
import com.otk.jesb.Folder;
import com.otk.jesb.FunctionEditor;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.Plan;
import com.otk.jesb.Solution;
import com.otk.jesb.Step;
import com.otk.jesb.Transition;
import com.otk.jesb.Debugger.PlanActivator;
import com.otk.jesb.activity.builtin.JDBCQueryActivity;
import com.otk.jesb.activity.builtin.WriteFileActivity;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FacadeOutline;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.Function;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.ParameterInitializer;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.instantiation.ValueMode;
import com.otk.jesb.resource.builtin.JDBCConnection;
import com.otk.jesb.util.SquigglePainter;

import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.NullableControl;
import xy.reflect.ui.control.swing.TextControl;
import xy.reflect.ui.control.swing.customizer.CustomizingFieldControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.customizer.CustomizingMethodControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.field.CapsuleFieldInfo;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.filter.IInfoFilter;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationItemInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.util.PrecomputedTypeInstanceWrapper;

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
				.getParameterInitializers().add(new ParameterInitializer(0, "tmp/test.txt"));
		((InstanceBuilder) ((ParameterInitializer) ab2.getInstanceBuilder().getRootInitializer()).getParameterValue())
				.getParameterInitializers().add(new ParameterInitializer(1,
						new Function("return (String)a.getRows().get(0).getCellValues().get(\"TABLE_NAME\");")));

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

			{
				if (object instanceof FacadeOutline) {
					Form rootInstanceBuilderFacadeForm = SwingRendererUtils
							.findDescendantFormsOfType(this, RootInstanceBuilderFacade.class.getName(), GUI.INSTANCE)
							.get(1);
					InstanceBuilderInitializerTreeControl initializerTreeControl = (InstanceBuilderInitializerTreeControl) rootInstanceBuilderFacadeForm
							.getFieldControlPlaceHolder("children").getFieldControl();
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
								return VisitStatus.BRANCH_VISIT_INTERRUPTED;
							}
							return VisitStatus.VISIT_NOT_INTERRUPTED;
						}
					});
				}
			}

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
						if (field.getName().equals("children") && (field.getType() instanceof IListTypeInfo)
								&& (((IListTypeInfo) field.getType()).getItemType() != null)
								&& ((IListTypeInfo) field.getType()).getItemType().getName()
										.equals(Facade.class.getName())) {
							return new InstanceBuilderInitializerTreeControl(GUI.this, this);
						}
						if (field.getName().equals("data") && (field.getType() instanceof IListTypeInfo)
								&& (((IListTypeInfo) field.getType()).getItemType() != null)
								&& ((IListTypeInfo) field.getType()).getItemType().getName()
										.equals(PathNode.class.getName())) {
							return new InstanceBuilderVariableTreeControl(GUI.this, this);
						}
						if (field.getName().equals("rootPathNodes") && (field.getType() instanceof IListTypeInfo)
								&& (((IListTypeInfo) field.getType()).getItemType() != null)
								&& ((IListTypeInfo) field.getType()).getItemType().getName()
										.equals(PathNode.class.getName())) {
							return new FunctionEditorVariableTreeControl(GUI.this, this);
						}
						if (field.getName().equals("functionBody")
								&& (field.getType().getName().equals(String.class.getName()))) {
							TextControl result = (TextControl) super.createFieldControl();
							result.getTextComponent().setTransferHandler(
									new FunctionEditorVariableTreeControl.PathImportTransferHandler());
							result.getTextComponent().setDropMode(DropMode.INSERT);
							return result;
						}
						if (field.getName().equals("facadeOutlineChildren")
								&& (field.getType() instanceof IListTypeInfo)
								&& (((IListTypeInfo) field.getType()).getItemType() != null)
								&& ((IListTypeInfo) field.getType()).getItemType().getName()
										.equals(FacadeOutline.class.getName())) {
							return new InstanceBuilderOutlineTreeControl(GUI.this, this);
						}
						if (field.getType().getName().equals(MappingsControl.Source.class.getName())) {
							return new MappingsControl();
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
							if (field.getName().equals("constructorGroup")) {
								if (((InstanceBuilderFacade) object).getConstructorSignatureOptions().size() <= 1) {
									setVisible(false);
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
									FunctionEditor.class.getName(), GUI.INSTANCE);
							TextControl textControl = (TextControl) SwingRendererUtils
									.findDescendantFieldControlPlaceHolder(functionEditorForm, "functionBody",
											GUI.INSTANCE)
									.getFieldControl();
							invocationData.getProvidedParameterValues().put(0,
									textControl.getTextComponent().getSelectionStart());
							invocationData.getProvidedParameterValues().put(1,
									textControl.getTextComponent().getSelectionEnd());
							return super.invoke(object, invocationData);
						}

					};
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
					TextControl textControl = (TextControl) SwingRendererUtils
							.findDescendantFieldControlPlaceHolder(this, "functionBody", GUI.INSTANCE)
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

	@Override
	public void openErrorDetailsDialog(Component activatorComponent, Throwable error) {
		openObjectDialog(activatorComponent, error);
	}
	
	
}
