package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.InstantiationUtils;

import xy.reflect.ui.info.type.DefaultTypeInfo;
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

	public IListTypeInfo getIterationListValueType(Facade parentFacade,
			List<VariableDeclaration> variableDeclarations) {
		Object iterationListValue = getIterationListValue();
		if (InstantiationUtils.getValueMode(iterationListValue) == ValueMode.PLAIN) {
			if (iterationListValue != null) {
				Class<? extends Object> iterableClass = iterationListValue.getClass();
				Object[] iterationListItems = ((IListTypeInfo) TypeInfoProvider.getTypeInfo(iterableClass))
						.toArray(iterationListValue);
				Class<?> baseItemClass = null;
				for (Object item : iterationListItems) {
					if (item == null) {
						continue;
					}
					if (baseItemClass == null) {
						baseItemClass = item.getClass();
					} else {
						Class<? extends Object> itemClass = item.getClass();
						while ((itemClass != null) && !itemClass.isAssignableFrom(baseItemClass)) {
							itemClass = itemClass.getSuperclass();
						}
						baseItemClass = (itemClass != null) ? itemClass : Object.class;
					}
				}
				if (baseItemClass == null) {
					baseItemClass = Object.class;
				}
				return (IListTypeInfo) TypeInfoProvider.getTypeInfo(iterableClass, new Class<?>[] { baseItemClass });
			}
		}
		if (getIterationListValueTypeName() != null) {
			return (IListTypeInfo) TypeInfoProvider.getTypeInfo(getIterationListValueTypeName());
		}
		if (iterationListValue instanceof InstantiationFunction) {
			InstantiationFunction function = (InstantiationFunction) iterationListValue;
			InstantiationFunctionCompilationContext compilationContext = new InstantiationFunctionCompilationContext(
					variableDeclarations, parentFacade);
			try {
				return (IListTypeInfo) function.guessReturnType(compilationContext.getPrecompiler(),
						compilationContext.getVariableDeclarations());
			} catch (CompilationError e) {
			}
		}
		return null;
	}

	public ITypeInfo getIterationVariableType(Facade parentFacade, List<VariableDeclaration> variableDeclarations) {
		if (getIterationVariableTypeName() != null) {
			return TypeInfoProvider.getTypeInfo(getIterationVariableTypeName());
		}
		IListTypeInfo listType = getIterationListValueType(parentFacade, variableDeclarations);
		if (listType != null) {
			return listType.getItemType();
		}
		return null;
	}

	public void validate(boolean recursively, List<VariableDeclaration> variableDeclarations, Facade parentFacade)
			throws ValidationError {
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
			Class<?> declaredItemClass = TypeInfoProvider.getClass(getIterationVariableTypeName());
			Class<?> inferredItemClass = null;
			{
				IListTypeInfo iterationListValueType = getIterationListValueType(parentFacade, variableDeclarations);
				if (iterationListValueType != null) {
					ITypeInfo itemType = iterationListValueType.getItemType();
					if (itemType != null) {
						inferredItemClass = ((DefaultTypeInfo) itemType).getJavaType();
					}
				}
			}
			if (inferredItemClass != null) {
				if (!(inferredItemClass.isPrimitive() ? ClassUtils.primitiveToWrapperClass(inferredItemClass)
						: inferredItemClass).isAssignableFrom(
								(declaredItemClass.isPrimitive() ? ClassUtils.primitiveToWrapperClass(declaredItemClass)
										: declaredItemClass))) {
					throw new IllegalStateException("Declared iteration variable type: '" + declaredItemClass.getName()
							+ "' does not inherit from item type of iteration list value type '"
							+ inferredItemClass.getName() + "'");

				}
			}
			if (InstantiationUtils.getValueMode(iterationListValue) == ValueMode.PLAIN) {
				if (iterationListValue != null) {
					IListTypeInfo iterationListTypeInfo = (IListTypeInfo) TypeInfoProvider
							.getTypeInfo(iterationListValue.getClass().getName());
					Object[] iterationListItems = iterationListTypeInfo.toArray(iterationListValue);
					for (Object iterationListItem : iterationListItems) {
						if (!(declaredItemClass.isPrimitive() ? ClassUtils.primitiveToWrapperClass(declaredItemClass)
								: declaredItemClass).isInstance(iterationListItem)) {
							throw new IllegalStateException("Iteration list item '" + iterationListItem
									+ "' not compatible with declared iteration variable type '"
									+ declaredItemClass.getName() + "'");
						}
					}
				}
			}
		}
		InstantiationUtils.validateValue(iterationListValue, getIterationListValueType(parentFacade, variableDeclarations), parentFacade,
				"iteration list value", recursively, variableDeclarations);
	}

}