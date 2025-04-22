package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.util.InstantiationUtils;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.ITypeInfo;

public class ParameterInitializerFacade extends Facade {

	private Facade parent;
	private int parameterPosition;

	public ParameterInitializerFacade(Facade parent, int parameterPosition) {
		this.parent = parent;
		this.parameterPosition = parameterPosition;
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
		ITypeInfo parentTypeInfo = getCurrentInstanceBuilderFacade().getTypeInfo();
		IMethodInfo constructor = InstantiationUtils.getConstructorInfo(parentTypeInfo,
				getCurrentInstanceBuilderFacade().getSelectedConstructorSignature());
		if (constructor == null) {
			throw new AssertionError();
		}
		return constructor.getParameters().get(parameterPosition);
	}

	@Override
	public ParameterInitializer getUnderlying() {
		ParameterInitializer result = ((InitializationCase) parent.getUnderlying())
				.getParameterInitializer(parameterPosition);
		if (result == null) {
			((InitializationCase) parent.getUnderlying()).getParameterInitializers()
					.add(result = new ParameterInitializer(parameterPosition, createDefaultParameterValue()));
		}
		return result;
	}

	public int getParameterPosition() {
		return parameterPosition;
	}

	public String getParameterName() {
		return getParameterInfo().getName();
	}

	public String getParameterTypeName() {
		return InstantiationUtils.makeTypeNamesRelative(getParameterInfo().getType().getName(),
				InstantiationUtils.getAncestorStructureInstanceBuilders(this));
	}

	public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().get();
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
		return true;
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
		} else {
			if (((InitializationCase) parent.getUnderlying()).getParameterInitializer(parameterPosition) != null) {
				((InitializationCase) parent.getUnderlying()).removeParameterInitializer(parameterPosition,
						getParameterInfo().getType().getName());
			}
		}
	}

	public ValueMode getParameterValueMode() {
		ParameterInitializer parameterInitializer = getUnderlying();
		return InstantiationUtils.getValueMode(parameterInitializer.getParameterValue());
	}

	public void setParameterValueMode(ValueMode valueMode) {
		setConcrete(true);
		IParameterInfo parameter = getParameterInfo();
		Object newParameterValue = InstantiationUtils.getDefaultInterpretableValue(parameter.getType(), valueMode, this);
		if (newParameterValue instanceof InstanceBuilder) {
			newParameterValue = new InstanceBuilderFacade(this, (InstanceBuilder) newParameterValue);
		}
		setParameterValue(newParameterValue);
	}

	public Object getParameterValue() {
		ParameterInitializer parameterInitializer = getUnderlying();
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
		IParameterInfo parameter = getParameterInfo();
		if ((value == null) && (parameter.getType().isPrimitive())) {
			throw new AssertionError("Cannot set null to primitive field");
		}
		ParameterInitializer parameterInitializer = getUnderlying();
		parameterInitializer.setParameterValue(value);
	}

	@Override
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		Object parameterValue = getUnderlying().getParameterValue();
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