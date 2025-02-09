package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.ITypeInfo;

public class ParameterInitializerFacade implements Facade {

	private Facade parent;
	private int parameterPosition;

	public ParameterInitializerFacade(Facade parent, int parameterPosition) {
		this.parent = parent;
		this.parameterPosition = parameterPosition;
	}

	@Override
	public Facade getParent() {
		return parent;
	}

	public IParameterInfo getParameterInfo() {
		ITypeInfo parentTypeInfo = getCurrentInstanceBuilderFacade().getTypeInfo();
		IMethodInfo constructor = MiscUtils.getConstructorInfo(parentTypeInfo,
				getCurrentInstanceBuilderFacade().getSelectedConstructorSignature());
		if (constructor == null) {
			throw new AssertionError();
		}
		return constructor.getParameters().get(parameterPosition);
	}

	@Override
	public ParameterInitializer getUnderlying() {
		ParameterInitializer result = ((InitializationCase) parent.getUnderlying())
				.getParameterInitializer(parameterPosition, getParameterInfo().getType().getName());
		if (result == null) {
			IParameterInfo parameter = getParameterInfo();
			result = new ParameterInitializer(parameterPosition, parameter.getType().getName(),
					MiscUtils.getDefaultInterpretableValue(parameter.getType(), this));
			((InitializationCase) parent.getUnderlying()).getParameterInitializers().add(result);
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
		return MiscUtils.makeTypeNamesRelative(getParameterInfo().getType().getName(),
				MiscUtils.getAncestorStructureInstanceBuilders(this));
	}

	public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().get();
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
			parent.setConcrete(true);
		}
	}

	public ValueMode getParameterValueMode() {
		ParameterInitializer parameterInitializer = getUnderlying();
		return MiscUtils.getValueMode(parameterInitializer.getParameterValue());
	}

	public void setParameterValueMode(ValueMode valueMode) {
		setConcrete(true);
		IParameterInfo parameter = getParameterInfo();
		Object parameterValue = MiscUtils.getDefaultInterpretableValue(parameter.getType(), valueMode, this);
		setParameterValue(parameterValue);
	}

	public Object getParameterValue() {
		ParameterInitializer parameterInitializer = getUnderlying();
		return MiscUtils.maintainInterpretableValue(parameterInitializer.getParameterValue(),
				getParameterInfo().getType());
	}

	public void setParameterValue(Object value) {
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
		Object parameterValue = getParameterValue();
		if (parameterValue instanceof InstanceBuilder) {
			result.add(new InstanceBuilderFacade(this, (InstanceBuilder) parameterValue));
		}
		return result;
	}

	@Override
	public String toString() {
		return "(" + getParameterInfo().getName() + ")";
	}

}