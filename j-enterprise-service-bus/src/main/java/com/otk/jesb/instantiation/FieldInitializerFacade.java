package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.util.InstantiationUtils;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.util.ReflectionUIUtils;

public class FieldInitializerFacade extends Facade {

	private Facade parent;
	private String fieldName;
	private Object fieldValue;

	public FieldInitializerFacade(Facade parent, String fieldName) {
		this.parent = parent;
		this.fieldName = fieldName;
		FieldInitializer fieldInitializer = getUnderlying();
		if (fieldInitializer == null) {
			this.fieldValue = createDefaultFieldValue();
		} else {
			this.fieldValue = fieldInitializer.getFieldValue();
		}
	}

	@Override
	public String express() {
		Object value = getFieldValue();
		if (value instanceof InstanceBuilderFacade) {
			value = ((InstanceBuilderFacade) value).getUnderlying();
		}
		String result = InstantiationUtils.express(value);
		if (getCondition() != null) {
			result = "IF " + InstantiationUtils.express(getCondition()) + ((result != null) ? (" THEN " + result) : "");
		}
		return result;
	}

	@Override
	public Facade getParent() {
		return parent;
	}

	public Object createDefaultFieldValue() {
		IFieldInfo field = getFieldInfo();
		return InstantiationUtils.getDefaultInterpretableValue(field.getType(), this);
	}

	public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().orElse(null);
	}

	public IFieldInfo getFieldInfo() {
		ITypeInfo parentTypeInfo = getCurrentInstanceBuilderFacade().getTypeInfo();
		return ReflectionUIUtils.findInfoByName(parentTypeInfo.getFields(), fieldName);
	}

	@Override
	public FieldInitializer getUnderlying() {
		return ((InitializationCase) parent.getUnderlying()).getFieldInitializer(fieldName);
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFieldTypeName() {
		return InstantiationUtils.makeTypeNamesRelative(getFieldInfo().getType().getName(),
				InstantiationUtils.getAncestorStructureInstanceBuilders(this));
	}

	public InstantiationFunction getCondition() {
		FieldInitializer fieldInitializer = getUnderlying();
		if (fieldInitializer == null) {
			return null;
		}
		return fieldInitializer.getCondition();
	}

	public void setCondition(InstantiationFunction condition) {
		setConcrete(true);
		FieldInitializer fieldInitializer = getUnderlying();
		if ((condition != null) && (condition.getFunctionBody() == null)) {
			condition = new InstantiationFunction("return true;");
		}
		fieldInitializer.setCondition(condition);
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
				((InitializationCase) parent.getUnderlying()).getFieldInitializers()
						.add(new FieldInitializer(fieldName, fieldValue));
			}
		} else {
			if (getUnderlying() != null) {
				((InitializationCase) parent.getUnderlying()).removeFieldInitializer(fieldName);
				fieldValue = createDefaultFieldValue();
			}
		}
	}

	public ValueMode getFieldValueMode() {
		FieldInitializer fieldInitializer = getUnderlying();
		if (fieldInitializer == null) {
			return null;
		}
		return InstantiationUtils.getValueMode(fieldInitializer.getFieldValue());
	}

	public void setFieldValueMode(ValueMode valueMode) {
		setConcrete(true);
		if (valueMode == getFieldValueMode()) {
			return;
		}
		IFieldInfo field = getFieldInfo();
		Object newFieldValue = InstantiationUtils.getDefaultInterpretableValue(field.getType(), valueMode, this);
		if (newFieldValue instanceof InstanceBuilder) {
			newFieldValue = new InstanceBuilderFacade(this, (InstanceBuilder) newFieldValue);
		}
		setFieldValue(newFieldValue);
	}

	public Object getFieldValue() {
		FieldInitializer fieldInitializer = getUnderlying();
		if (fieldInitializer == null) {
			return null;
		}
		Object result = InstantiationUtils.maintainInterpretableValue(fieldInitializer.getFieldValue(),
				getFieldInfo().getType());
		if (result instanceof InstanceBuilder) {
			result = new InstanceBuilderFacade(this, (InstanceBuilder) result);
		}
		return result;
	}

	public void setFieldValue(Object value) {
		if (value instanceof InstanceBuilderFacade) {
			value = ((InstanceBuilderFacade) value).getUnderlying();
		}
		setConcrete(true);
		FieldInitializer fieldInitializer = getUnderlying();
		IFieldInfo field = getFieldInfo();
		if ((value == null) && (field.getType().isPrimitive())) {
			throw new AssertionError("Cannot set null to primitive field");
		}
		fieldInitializer.setFieldValue(fieldValue = value);
	}

	@Override
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		if (fieldValue instanceof InstanceBuilder) {
			result.addAll(new InstanceBuilderFacade(this, (InstanceBuilder) fieldValue).getChildren());
		}
		return result;
	}

	@Override
	public String toString() {
		return fieldName + ((getCondition() != null) ? "?" : "");
	}

}