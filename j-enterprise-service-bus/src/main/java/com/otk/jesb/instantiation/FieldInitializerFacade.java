package com.otk.jesb.instantiation;

import java.util.List;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.InstantiationUtils;

import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.util.ReflectionUIUtils;

public class FieldInitializerFacade extends InitializerFacade {

	private String fieldName;
	private IFieldInfo fieldInfo;

	public FieldInitializerFacade(Facade parent, String fieldName, Solution solutionInstance) {
		super(parent, solutionInstance);
		this.fieldName = fieldName;
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
		if (getCondition() == function) {
			return baseResult;
		}
		if (getFieldValue() == function) {
			return result;
		}
		throw new UnexpectedError();
	}

	@Override
	public Class<?> getFunctionReturnType(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		if (getCondition() == function) {
			return boolean.class;
		}
		if (getFieldValue() == function) {
			return ((DefaultTypeInfo) getFieldInfo().getType()).getJavaType();
		}
		throw new UnexpectedError();
	}

	@Override
	public void validate(boolean recursively, List<VariableDeclaration> variableDeclarations) throws ValidationError {
		if (!isConcrete()) {
			return;
		}
		if (getCondition() != null) {
			InstantiationUtils.validateValue(getCondition(), TypeInfoProvider.getTypeInfo(boolean.class), getParent(),
					"condition", true, variableDeclarations, solutionInstance);
		}
		InstantiationUtils.validateValue(getUnderlying().getFieldValue(), getFieldInfo().getType(), this, "field value",
				recursively, variableDeclarations, solutionInstance);
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

	public Object createDefaultFieldValue() {
		return super.createDefaultValue();
	}

	public IFieldInfo getFieldInfo() {
		if (fieldInfo == null) {
			ITypeInfo parentTypeInfo = getCurrentInstanceBuilderFacade().getTypeInfo(solutionInstance);
			fieldInfo = ReflectionUIUtils.findInfoByName(parentTypeInfo.getFields(), fieldName);
		}
		return fieldInfo;
	}

	@Override
	public FieldInitializer getUnderlying() {
		return ((InitializationCase) getParent().getUnderlying()).getFieldInitializer(fieldName);
	}

	@Override
	protected void createUnderlying(Object value) {
		((InitializationCase) getParent().getUnderlying()).getFieldInitializers()
				.add(new FieldInitializer(fieldName, value));
	}

	@Override
	protected void deleteUnderlying() {
		((InitializationCase) getParent().getUnderlying()).removeFieldInitializer(fieldName);
	}

	@Override
	protected Object retrieveInitializerValue(Object initializer) {
		return ((FieldInitializer) initializer).getFieldValue();
	}

	@Override
	protected void updateInitializerValue(Object initializer, Object newValue) {
		((FieldInitializer) initializer).setFieldValue(newValue);
	}

	@Override
	protected ITypeInfo getValueType() {
		return getFieldInfo().getType();
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFieldTypeName() {
		return super.getValueTypeName();
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

	public ValueMode getFieldValueMode() {
		return super.getValueMode();
	}

	public void setFieldValueMode(ValueMode valueMode) {
		super.setValueMode(valueMode);
	}

	public Object getFieldValue() {
		return super.getValue();
	}

	public void setFieldValue(Object value) {
		super.setValue(value);
	}

	@Override
	public String toString() {
		return fieldName + ((getCondition() != null) ? "?" : "");
	}

}