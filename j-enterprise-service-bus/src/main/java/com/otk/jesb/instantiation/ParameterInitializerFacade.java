package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.util.InstantiationUtils;

import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;

public class ParameterInitializerFacade extends Facade {

	private Facade parent;
	private int parameterPosition;
	private Object parameterValue;
	private IParameterInfo parameterInfo;

	public ParameterInitializerFacade(Facade parent, int parameterPosition) {
		this.parent = parent;
		this.parameterPosition = parameterPosition;
		ParameterInitializer parameterInitializer = getUnderlying();
		if (parameterInitializer == null) {
			this.parameterValue = createDefaultParameterValue();
		} else {
			this.parameterValue = parameterInitializer.getParameterValue();
		}
	}

	@Override
	public List<VariableDeclaration> getAdditionalVariableDeclarations(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		List<VariableDeclaration> baseResult = parent.getAdditionalVariableDeclarations(null, baseVariableDeclarations);
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
		if (!isConcrete()) {
			throw new ValidationError("Plain value or function required");
		}
		InstantiationUtils.validateValue(getUnderlying().getParameterValue(), getParameterInfo().getType(), this,
				"parameter value", recursively, variableDeclarations);
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

	@Override
	public Facade getParent() {
		return parent;
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
		return ((InitializationCase) parent.getUnderlying()).getParameterInitializer(parameterPosition);
	}

	public int getParameterPosition() {
		return parameterPosition;
	}

	public String getParameterName() {
		return getParameterInfo().getName();
	}

	public String getParameterTypeName() {
		return InstantiationUtils.makeTypeNamesRelative(getParameterInfo().getType().getName(),
				InstantiationUtils.getAncestorStructuredInstanceBuilders(this));
	}

	public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().orElse(null);
	}

	public Object createDefaultParameterValue() {
		IParameterInfo parameter = getParameterInfo();
		return InstantiationUtils.getDefaultInterpretableValue(parameter.getType(), this);
	}

	@Override
	public boolean isConcrete() {
		if (!parent.isConcrete()) {
			return false;
		}
		return getUnderlying() != null;
	}

	@Override
	public void setConcrete(boolean b) {
		if (b == isConcrete()) {
			return;
		}
		if (b) {
			if (!parent.isConcrete()) {
				parent.setConcrete(true);
			}
			if (getUnderlying() == null) {
				((InitializationCase) parent.getUnderlying()).getParameterInitializers()
						.add(new ParameterInitializer(parameterPosition, parameterValue));
			}
		} else {
			if (getUnderlying() != null) {
				((InitializationCase) parent.getUnderlying()).removeParameterInitializer(parameterPosition);
				parameterValue = createDefaultParameterValue();
			}
		}
	}

	public ValueMode getParameterValueMode() {
		ParameterInitializer parameterInitializer = getUnderlying();
		if (parameterInitializer == null) {
			return null;
		}
		return InstantiationUtils.getValueMode(parameterInitializer.getParameterValue());
	}

	public void setParameterValueMode(ValueMode valueMode) {
		if (valueMode == null) {
			setConcrete(false);
		} else {
			setConcrete(true);
			IParameterInfo parameter = getParameterInfo();
			Object newParameterValue = InstantiationUtils.getDefaultInterpretableValue(parameter.getType(), valueMode,
					this);
			ParameterInitializer parameterInitializer = getUnderlying();
			parameterInitializer.setParameterValue(newParameterValue);
		}
	}

	public Object getParameterValue() {
		ParameterInitializer parameterInitializer = getUnderlying();
		if (parameterInitializer == null) {
			return null;
		}
		Object result = InstantiationUtils.maintainInterpretableValue(parameterInitializer.getParameterValue(),
				getParameterInfo().getType());
		if (result instanceof InstanceBuilder) {
			result = new InstanceBuilderFacade(this, (InstanceBuilder) result);
		}
		return result;
	}

	public void setParameterValue(Object value) {
		if (value instanceof InstanceBuilderFacade) {
			value = ((InstanceBuilderFacade) value).getUnderlying();
		}
		setConcrete(true);
		ParameterInitializer parameterInitializer = getUnderlying();
		IParameterInfo parameter = getParameterInfo();
		if ((value == null) && (parameter.getType().isPrimitive())) {
			throw new UnexpectedError("Cannot set null to a primitive field");
		}
		parameterInitializer.setParameterValue(parameterValue = value);
	}

	@Override
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		if (parameterValue instanceof InstanceBuilder) {
			result.addAll(new InstanceBuilderFacade(this, (InstanceBuilder) parameterValue).getChildren());
		}
		return result;
	}

	@Override
	public String toString() {
		return "(" + getParameterInfo().getName() + ")";
	}

}