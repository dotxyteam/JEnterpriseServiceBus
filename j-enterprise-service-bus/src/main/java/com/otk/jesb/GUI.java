package com.otk.jesb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.otk.jesb.InstanceSpecification.DynamicValue;

import xy.reflect.ui.CustomizedUI;
import xy.reflect.ui.control.swing.customizer.CustomizingForm;
import xy.reflect.ui.control.swing.customizer.SwingCustomizer;
import xy.reflect.ui.info.filter.IInfoFilter;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.method.MethodInfoProxy;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.parameter.ParameterInfoProxy;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.factory.InfoProxyFactory;

public class GUI extends SwingCustomizer {

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
	public CustomizingForm createForm(Object object, IInfoFilter infoFilter) {

		return new CustomizingForm(this, object, infoFilter) {

			private static final long serialVersionUID = 1L;

		};
	}

	public static class Reflection extends CustomizedUI {

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
			}.wrapTypeInfo(super.getTypeInfoBeforeCustomizations(type));
		}

	}

}
