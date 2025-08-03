package com.otk.jesb.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;

import com.formdev.flatlaf.ui.FlatTableHeaderUI;
import com.formdev.flatlaf.util.UIScale;
import com.otk.jesb.Debugger;
import com.otk.jesb.FunctionEditor;
import com.otk.jesb.JESB;
import com.otk.jesb.PathExplorer.PathNode;
import com.otk.jesb.Preferences;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.Debugger.PlanActivation;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.Facade;
import com.otk.jesb.instantiation.FacadeOutline;
import com.otk.jesb.instantiation.FieldInitializerFacade;
import com.otk.jesb.instantiation.InstanceBuilderFacade;
import com.otk.jesb.instantiation.ListItemInitializerFacade;
import com.otk.jesb.instantiation.ParameterInitializerFacade;
import com.otk.jesb.instantiation.RootInstanceBuilderFacade;
import com.otk.jesb.instantiation.ValueMode;
import com.otk.jesb.util.FadingPanel;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.SquigglePainter;
import de.sciss.syntaxpane.syntaxkits.JavaSyntaxKit;
import xy.reflect.ui.control.swing.ListControl;
import xy.reflect.ui.control.swing.NullableControl;
import xy.reflect.ui.control.swing.TextControl;
import xy.reflect.ui.control.swing.builder.AbstractEditorBuilder;
import xy.reflect.ui.control.swing.builder.AbstractEditorFormBuilder;
import xy.reflect.ui.control.swing.customizer.CustomizingFieldControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.customizer.CustomizingMethodControlPlaceHolder;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;
import xy.reflect.ui.control.swing.plugin.EditorPlugin;
import xy.reflect.ui.control.swing.renderer.Form;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.ValidationSession;
import xy.reflect.ui.info.field.MembersCapsuleFieldInfo;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.filter.IInfoFilter;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.factory.EncapsulatedObjectFactory;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.item.BufferedItemPosition;
import xy.reflect.ui.util.PrecomputedTypeInstanceWrapper;
import xy.reflect.ui.util.SystemProperties;

public class GUI extends SwingCustomizer {

	static {
		if (JESB.DEBUG) {
			System.setProperty(SystemProperties.DEBUG, Boolean.TRUE.toString());
		}
		Preferences.INSTANCE.getTheme().activate();
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
		JESBReflectionUI.onFormCreationStart(object);
		try {
			return new CustomizingForm(this, object, infoFilter) {

				private static final long serialVersionUID = 1L;

				{
					if (object instanceof FacadeOutline) {
						Form rootInstanceBuilderFacadeForm = SwingRendererUtils.findDescendantFormsOfType(this,
								RootInstanceBuilderFacade.class.getName(), GUI.INSTANCE).get(1);
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
								return new InstanceBuilderOutlineTreeControl(GUI.this, this);
							}
							if (field.getType().getName().equals(MappingsControl.Source.class.getName())) {
								return new MappingsControl(swingRenderer, this);
							}
							if (objectType.getName().equals(
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
							if (objectType.getName().equals(
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
											if (facade.getParameterValueMode() == ValueMode.PLAIN) {
												return facade.createDefaultParameterValue();
											} else {
												return super.getNewValue();
											}
										}

									};
								}
							}
							if (objectType.getName().equals(
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
		} finally {
			JESBReflectionUI.onFormCreationEnd(object);
		}
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
		if (JESB.DEBUG) {
			error.printStackTrace();
		}
		openObjectDialog(activatorComponent, error);
	}

	public static class BetterFlatTableHeaderUI extends FlatTableHeaderUI {

		@Override
		public Dimension getPreferredSize(JComponent c) {
			// replace Dimension size = super.getPreferredSize( c );
			BasicTableHeaderUI superObject = new BasicTableHeaderUI() {
				{
					header = (JTableHeader) c;
				}
			};
			Dimension size = superObject.getPreferredSize(c);
			if (size.height > 0) {
				Insets insets = c.getInsets();
				if ((insets == null) || (size.height > (insets.top + insets.bottom))) {
					// replace UIScale.scale(height) by UIScale.scale(size.height)
					size.height = Math.max(size.height, UIScale.scale(size.height));
				}
			}
			return size;
		}

		public static ComponentUI createUI(JComponent c) {
			return new BetterFlatTableHeaderUI();
		}
	}
}
