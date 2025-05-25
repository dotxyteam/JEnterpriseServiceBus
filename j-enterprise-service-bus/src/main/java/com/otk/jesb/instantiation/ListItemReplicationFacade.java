package com.otk.jesb.instantiation;

import java.util.ArrayList;
import com.otk.jesb.ValidationError;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.InstantiationUtils;

import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.util.ClassUtils;

public class ListItemReplicationFacade {

	private ListItemReplication listItemReplication;

	public ListItemReplicationFacade() {
		this.listItemReplication = new ListItemReplication();
	}

	public ListItemReplicationFacade(ListItemReplication listItemReplication) {
		this.listItemReplication = listItemReplication;
	}

	public ListItemReplication getUnderlying() {
		return listItemReplication;
	}

	public String getIterationVariableName() {
		return listItemReplication.getIterationVariableName();
	}

	public void setIterationVariableName(String iterationVariableName) {
		listItemReplication.setIterationVariableName(iterationVariableName);
	}

	public Object getIterationListValue() {
		return InstantiationUtils.maintainInterpretableValue(listItemReplication.getIterationListValue(),
				TypeInfoProvider.getTypeInfo(Object.class.getName()));
	}

	public void setIterationListValue(Object iterationListValue) {
		listItemReplication.setIterationListValue(iterationListValue);
	}

	public String getIterationListValueTypeName() {
		return listItemReplication.getIterationListValueTypeName();
	}

	public void setIterationListValueTypeName(String iterationListValueTypeName) {
		listItemReplication.setIterationListValueTypeName(iterationListValueTypeName);
	}

	public String getIterationVariableTypeName() {
		return listItemReplication.getIterationVariableTypeName();
	}

	public void setIterationVariableTypeName(String iterationVariableTypeName) {
		listItemReplication.setIterationVariableTypeName(iterationVariableTypeName);
	}

	public ValueMode getIterationListValueMode() {
		return InstantiationUtils.getValueMode(getIterationListValue());
	}

	public void setIterationListValueMode(ValueMode valueMode) {
		Object iterationListValue;
		if (valueMode == ValueMode.FUNCTION) {
			String functionBody;
			if (getIterationListValueTypeName() != null) {
				Class<?> listClass = TypeInfoProvider.getClass(getIterationListValueTypeName());
				if (listClass.isArray()) {
					functionBody = "return new " + listClass.getComponentType().getName() + "[]{};";
				} else {
					functionBody = "return new " + listClass.getName() + "();";
				}
			} else {
				functionBody = "return new " + ArrayList.class.getName() + "<Object>();";
			}
			iterationListValue = new InstantiationFunction(functionBody);
		} else {
			iterationListValue = new ArrayList<Object>();
		}
		setIterationListValue(iterationListValue);
	}

	public Class<?> getIterationListValueClass() {
		Object iterationListValue = getIterationListValue();
		if (InstantiationUtils.getValueMode(iterationListValue) == ValueMode.PLAIN) {
			if (iterationListValue != null) {
				return iterationListValue.getClass();
			}
		}
		if (getIterationListValueTypeName() != null) {
			return TypeInfoProvider.getClass(getIterationListValueTypeName());
		}
		return Object.class;
	}

	public Class<?> getIterationVariableClass() {
		if (getIterationVariableTypeName() != null) {
			return TypeInfoProvider.getClass(getIterationVariableTypeName());
		}
		Class<?> listClass = getIterationListValueClass();
		if (listClass.isArray()) {
			return listClass.getComponentType();
		}
		return Object.class;
	}

	public void validate() throws ValidationError {
		Object iterationListValue = getIterationListValue();
		if (InstantiationUtils.getValueMode(iterationListValue) == ValueMode.PLAIN) {
			if (iterationListValue != null) {
				if (!(TypeInfoProvider.getTypeInfo(iterationListValue.getClass().getName()) instanceof IListTypeInfo)) {
					throw new IllegalStateException(
							"Unexpected iteration list value type '" + iterationListValue.getClass().getName()
									+ "': Expected array or standard collection type");
				}
			}
		}
		String iterationListValueTypeName = getIterationListValueTypeName();
		if (iterationListValueTypeName != null) {
			ITypeInfo declaredIterationListTypeInfo = TypeInfoProvider.getTypeInfo(iterationListValueTypeName);
			if (!(declaredIterationListTypeInfo instanceof IListTypeInfo)) {
				throw new IllegalStateException("Unexpected iteration list value type '"
						+ declaredIterationListTypeInfo.getName() + "': Expected array or standard collection type");
			}
			if (InstantiationUtils.getValueMode(iterationListValue) == ValueMode.PLAIN) {
				if (iterationListValue != null) {
					if (!declaredIterationListTypeInfo.supports(iterationListValue)) {
						throw new IllegalStateException(
								"Iteration list value not compatible with declared type: '" + iterationListValue
										+ "' is not an instance of '" + declaredIterationListTypeInfo.getName() + "'");

					}
				}
			}
		}
		if (getIterationVariableTypeName() != null) {
			Class<?> declaredIterationVariableClass = TypeInfoProvider.getClass(getIterationVariableTypeName());
			Class<?> listClass = getIterationListValueClass();
			if (listClass.isArray()) {
				Class<?> arrayComponentClass = listClass.getComponentType();
				if (!(arrayComponentClass.isPrimitive() ? ClassUtils.primitiveToWrapperClass(arrayComponentClass)
						: arrayComponentClass)
								.isAssignableFrom((declaredIterationVariableClass.isPrimitive()
										? ClassUtils.primitiveToWrapperClass(declaredIterationVariableClass)
										: declaredIterationVariableClass))) {
					throw new IllegalStateException(
							"Declared iteration variable type: '" + declaredIterationVariableClass.getName()
									+ "' does not inherit from item type of iteration list value type '"
									+ listClass.getComponentType().getName() + "'");

				}
			}
			if (InstantiationUtils.getValueMode(iterationListValue) == ValueMode.PLAIN) {
				if (iterationListValue != null) {
					IListTypeInfo iterationListTypeInfo = (IListTypeInfo) TypeInfoProvider
							.getTypeInfo(iterationListValue.getClass().getName());
					Object[] iterationListItems = iterationListTypeInfo.toArray(iterationListValue);
					for (Object iterationListItem : iterationListItems) {
						if (!(declaredIterationVariableClass.isPrimitive()
								? ClassUtils.primitiveToWrapperClass(declaredIterationVariableClass)
								: declaredIterationVariableClass).isInstance(iterationListItem)) {
							throw new IllegalStateException("Iteration list item '" + iterationListItem
									+ "' not compatible with declared iteration variable type '"
									+ declaredIterationVariableClass.getName() + "'");
						}
					}
				}
			}
		}

	}

}