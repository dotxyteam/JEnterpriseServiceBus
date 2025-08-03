package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.PotentialError;
import com.otk.jesb.util.InstantiationUtils;

import xy.reflect.ui.info.type.ITypeInfo;

public abstract class InitializerFacade extends Facade {

	private static Object UNDEFINED_ABSTRACT_VALUE = new Object() {
		@Override
		public String toString() {
			return InitializerFacade.class.getName() + ".UNDEFINED_ABSTRACT_VALUE";
		}
	};

	private Facade parent;
	private Object abstractValue = UNDEFINED_ABSTRACT_VALUE;

	protected abstract Object retrieveInitializerValue(Object initializer);

	protected abstract void updateInitializerValue(Object initializer, Object newValue);

	protected abstract ITypeInfo getValueType();

	protected abstract void createUnderlying(Object value);

	protected abstract void deleteUnderlying();

	public InitializerFacade(Facade parent) {
		this.parent = parent;
	}

	@Override
	public Facade getParent() {
		return parent;
	}

	protected String getValueTypeName() {
		return InstantiationUtils.makeTypeNamesRelative(getValueType().getName(),
				InstantiationUtils.getAncestorInstanceBuilders(this));
	}

	public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().orElse(null);
	}

	protected Object createDefaultValue() {
		return InstantiationUtils.getDefaultInterpretableValue(getValueType(), this);
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
		if (b) {
			if (!parent.isConcrete()) {
				parent.setConcrete(true);
			}
			if (getUnderlying() == null) {
				if (abstractValue == UNDEFINED_ABSTRACT_VALUE) {
					abstractValue = createDefaultValue();
				}
				createUnderlying(abstractValue);
			}
		} else {
			if (getUnderlying() != null) {
				deleteUnderlying();
				abstractValue = createDefaultValue();
			}
		}
	}

	protected ValueMode getValueMode() {
		Object initializer = getUnderlying();
		if (initializer == null) {
			return null;
		}
		return InstantiationUtils.getValueMode(retrieveInitializerValue(initializer));
	}

	protected void setValueMode(ValueMode valueMode) {
		if (valueMode == null) {
			setConcrete(false);
		} else {
			setConcrete(true);
			Object newValue = InstantiationUtils.getDefaultInterpretableValue(getValueType(), valueMode, this);
			Object initializer = getUnderlying();
			updateInitializerValue(initializer, newValue);
		}
	}

	protected Object getValue() {
		Object initializer = getUnderlying();
		if (initializer == null) {
			return null;
		}
		Object result = InstantiationUtils.maintainInterpretableValue(retrieveInitializerValue(initializer),
				getValueType());
		if (result instanceof InstanceBuilder) {
			result = new InstanceBuilderFacade(this, (InstanceBuilder) result);
		}
		return result;
	}

	protected void setValue(Object value) {
		if (value instanceof InstanceBuilderFacade) {
			value = ((InstanceBuilderFacade) value).getUnderlying();
		}
		setConcrete(true);
		Object initializer = getUnderlying();
		if ((value == null) && (getValueType().isPrimitive())) {
			throw new PotentialError("Cannot set null to a primitive field");
		}
		updateInitializerValue(initializer, abstractValue = value);
	}

	@Override
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		if (abstractValue == UNDEFINED_ABSTRACT_VALUE) {
			Object initializer = getUnderlying();
			if (initializer == null) {
				this.abstractValue = createDefaultValue();
			} else {
				this.abstractValue = retrieveInitializerValue(initializer);
			}
		}
		if (abstractValue instanceof InstanceBuilder) {
			result.addAll(new InstanceBuilderFacade(this, (InstanceBuilder) abstractValue).getChildren());
		}
		return result;
	}

}