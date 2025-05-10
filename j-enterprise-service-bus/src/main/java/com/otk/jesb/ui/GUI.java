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
import com.otk.jesb.FunctionEditor;
import com.otk.jesb.JESB;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.Debugger.PlanActivator;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FacadeOutline;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.instantiation.ValueMode;
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
import xy.reflect.ui.info.ValidationSession;
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
import xy.reflect.ui.util.SystemProperties;

public class GUI extends SwingCustomizer {

	static {
		if (JESB.DEBUG) {
			System.setProperty(SystemProperties.DEBUG, Boolean.TRUE.toString());
		}
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
				throw new UnexpectedError(e);
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
								return VisitStatus.SUBTREE_VISIT_INTERRUPTED;
							}
							return VisitStatus.VISIT_NOT_INTERRUPTED;
						}
					});
				}
			}

			@Override
			protected CustomizingFieldControlPlaceHolder createFieldControlPlaceHolder(IFieldInfo field) {
				return new CustomizingFieldControlPlaceHolder(this, field) {

					private static final long serialVersionUID = 1L;

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
							return new MappingsControl(swingRenderer, this);
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
							ListControl planActivatorsControl = (ListControl) debuggerForm
									.getFieldControlPlaceHolder("planActivators").getFieldControl();
							planActivatorsControl.refreshUI(false);
							Form currentPlanActivatorForm = SwingRendererUtils.findAncestorFormOfType(thisForm,
									PlanActivator.class.getName(), swingRenderer);
							PlanActivator currentPlanActivator = (PlanActivator) currentPlanActivatorForm.getObject();
							BufferedItemPosition planActivatorPosition = planActivatorsControl
									.findItemPositionByReference(currentPlanActivator);
							BufferedItemPosition lastPlanExecutorPosition = planActivatorPosition.getSubItemPositions()
									.get(planActivatorPosition.getSubItemPositions().size() - 1);
							planActivatorsControl.setSingleSelection(lastPlanExecutorPosition);
						}
					};
				}
				return super.createMethodControlPlaceHolder(method);
			}

			@Override
			public void validateForm(ValidationSession session) throws Exception {
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
					super.validateForm(session);
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
		if (JESB.DEBUG) {
			error.printStackTrace();
		}
		openObjectDialog(activatorComponent, error);
	}

}
