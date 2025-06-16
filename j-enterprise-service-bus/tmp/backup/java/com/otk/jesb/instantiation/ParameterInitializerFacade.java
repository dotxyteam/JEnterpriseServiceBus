package com.otk.jesb.instantiation;

import java.util.List;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.util.InstantiationUtils;

import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;

public class ParameterInitializerFacade extends InitializerFacade {

	private int parameterPosition;
	private IParameterInfo parameterInfo;

	public ParameterInitializerFacade(Facade parent, int parameterPosition) {
		super(parent);
		this.parameterPosition = parameterPosition;
	}

	@Override
	public List<VariableDeclaration> getAdditionalVariableDeclarations(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		List<VariableDeclaration> baseResult = getParent().getAdditionalVariableDeclarations(null,
				baseVariableDeclarations);
		List<VariableDeclaration> result = baseResult;
		if (function == null) {
			return result;
		}
		if (getParameterValue() == function) {
			return result;
		}
		throw new UnexpectedError();
	}

	@Override
	public Class<?> getFunctionReturnType(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		if (getParameterValue() == function) {
			return ((DefaultTypeInfo) getParameterInfo().getType()).getJavaType();
		}
		throw new UnexpectedError();
	}

	@Override
	public void validate(boolean recursively, List<VariableDeclaration> variableDeclarations) throws ValidationError {
		if (!getParent().isConcrete()) {
			return;
		}
		if (!isConcrete()) {
			throw new ValidationError("Parameter value specification required");
		}
		InstantiationUtils.validateValue(getUnderlying().getParameterValue(), getParameterInfo().getType(), this,
				"parameter value", recursively, variableDeclarations);
	}

	@Override
	public boolean isValidable() {
		return getParent().isConcrete();
	}

	@Override
	public String express() {
		Object value = getParameterValue();
		if (value instanceof InstanceBuilderFacade) {
			value = ((InstanceBuilderFacade) value).getUnderlying();
		}
		String result = InstantiationUtils.express(value);
		return result;
	}

	public IParameterInfo getParameterInfo() {
		if (parameterInfo == null) {
			ITypeInfo parentTypeInfo = getCurrentInstanceBuilderFacade().getTypeInfo();
			IMethodInfo constructor = InstantiationUtils.getConstructorInfo(parentTypeInfo,
					getCurrentInstanceBuilderFacade().getSelectedConstructorSignature());
			if (constructor == null) {
				throw new UnexpectedError();
			}
			parameterInfo = constructor.getParameters().get(parameterPosition);
		}
		return parameterInfo;
	}

	@Override
	public ParameterInitializer getUnderlying() {
		return ((InitializationCase) getParent().getUnderlying()).getParameterInitializer(parameterPosition);
	}

	@Override
	protected Object retrieveInitializerValue(Object initializer) {
		return ((ParameterInitializer) initializer).getParameterValue();
	}

	@Override
	protected void updateInitializerValue(Object initializer, Object newValue) {
		((ParameterInitializer) initializer).setParameterValue(newValue);
	}

	@Override
	protected ITypeInfo getValueType() {
		return getParameterInfo().getType();
	}

	@Override
	protected void createUnderlying(Object value) {
		((InitializationCase) getParent().getUnderlying()).getParameterInitializers()
				.add(new ParameterInitializer(parameterPosition, value));
	}

	@Override
	protected void deleteUnderlying() {
		((InitializationCase) getParent().getUnderlying()).removeParameterInitializer(parameterPosition);
	}

	public int getParameterPosition() {
		return parameterPosition;
	}

	public String getParameterName() {
		return getParameterInfo().getName();
	}

	public String getParameterTypeName() {
		return super.getValueTypeName();
	}

	public Object createDefaultParameterValue() {
		return super.createDefaultValue();
	}

	public ValueMode getParameterValueMode() {
		return super.getValueMode();
	}

	public void setParameterValueMode(ValueMode valueMode) {
		super.setValueMode(valueMode);
	}

	public Object getParameterValue() {
		return super.getValue();
	}

	public void setParameterValue(Object value) {
		super.setValue(value);
	}

	@Override
	public String toString() {
		return "(" + getParameterInfo().getName() + ")";
	}

}