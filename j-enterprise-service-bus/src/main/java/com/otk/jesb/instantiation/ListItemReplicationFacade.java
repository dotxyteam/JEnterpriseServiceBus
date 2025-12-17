package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.InstantiationUtils;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.util.ClassUtils;

public class ListItemReplicationFacade {

	private ListItemInitializerFacade listItemInitializerFacade;
	private ListItemReplication listItemReplication;
	private Solution solutionInstance;

	public ListItemReplicationFacade(ListItemInitializerFacade listItemInitializerFacade, Solution solutionInstance) {
		this.listItemInitializerFacade = listItemInitializerFacade;
		this.listItemReplication = new ListItemReplication();
		this.solutionInstance = solutionInstance;
	}

	public ListItemReplicationFacade(ListItemInitializerFacade listItemInitializerFacade,
			ListItemReplication listItemReplication, Solution solutionInstance) {
		this.listItemInitializerFacade = listItemInitializerFacade;
		this.listItemReplication = listItemReplication;
		this.solutionInstance = solutionInstance;
	}

	public ListItemReplication getUnderlying() {
		return listItemReplication;
	}

	public ListItemInitializerFacade getListItemInitializerFacade() {
		return listItemInitializerFacade;
	}

	public String getIterationVariableName() {
		return listItemReplication.getIterationVariableName();
	}

	public void setIterationVariableName(String iterationVariableName) {
		listItemReplication.setIterationVariableName(iterationVariableName);
	}

	public Object getIterationListValue() {
		return InstantiationUtils.maintainInterpretableValue(listItemReplication.getIterationListValue(),
				TypeInfoProvider.getTypeInfo(Object.class.getName(), solutionInstance));
	}

	public void setIterationListValue(Object iterationListValue) {
		listItemReplication.setIterationListValue(iterationListValue);
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
			String functionBody = "return new " + ArrayList.class.getName() + "<Object>();";
			iterationListValue = new InstantiationFunction(functionBody);
		} else {
			iterationListValue = new ArrayList<Object>();
		}
		setIterationListValue(iterationListValue);
	}

	public ITypeInfo getDeclaredIterationVariableTypeInfo() {
		String iterationVariableTypeName = getIterationVariableTypeName();
		if (iterationVariableTypeName != null) {
			return TypeInfoProvider.getTypeInfo(getIterationVariableTypeName(), solutionInstance);
		}
		return null;
	}

	public ITypeInfo guessIterationVariableTypeInfo(List<VariableDeclaration> variableDeclarations) {
		Object iterationListValue = getIterationListValue();
		if (iterationListValue instanceof InstantiationFunction) {
			InstantiationFunction function = (InstantiationFunction) iterationListValue;
			InstantiationFunctionCompilationContext compilationContext = new InstantiationFunctionCompilationContext(
					variableDeclarations, listItemInitializerFacade);
			try {
				ITypeInfo type = function.guessReturnTypeInfo(compilationContext.getPrecompiler(),
						compilationContext.getVariableDeclarations(function));
				if (!(type instanceof IListTypeInfo)) {
					throw new IllegalStateException("Invalid iteration list function return type '" + type.getName()
							+ "': Expected array or standard collection type");
				}
				return ((IListTypeInfo) type).getItemType();
			} catch (CompilationError e) {
				return null;
			}
		} else {
			if (iterationListValue != null) {
				ITypeInfo actualIterationListType = TypeInfoProvider.getTypeInfo(iterationListValue.getClass());
				if (!(actualIterationListType instanceof IListTypeInfo)) {
					throw new IllegalStateException(
							"Invalid iteration list value type '" + iterationListValue.getClass().getName()
									+ "': Expected array or standard collection type");
				}
				Object[] iterationListItems = ((IListTypeInfo) actualIterationListType).toArray(iterationListValue);
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
					return null;
				} else {
					return TypeInfoProvider.getTypeInfo(baseItemClass, new Class<?>[] { baseItemClass },
							solutionInstance);
				}
			} else {
				return null;
			}
		}
	}

	public String preprendExpression(String baseExpression) {
		return "FOR " + getIterationVariableName() + " IN " + InstantiationUtils.express(getIterationListValue())
				+ ((baseExpression != null) ? (" LOOP " + baseExpression) : "");
	}

	public void validate(List<VariableDeclaration> variableDeclarations) throws ValidationError {
		try {
			ITypeInfo guessedIterationVariableType = guessIterationVariableTypeInfo(variableDeclarations);
			ITypeInfo declaredIterationVariableType = getDeclaredIterationVariableTypeInfo();
			if ((guessedIterationVariableType != null) && (declaredIterationVariableType != null)) {
				Class<?> declaredItemClass = ((DefaultTypeInfo) declaredIterationVariableType).getJavaType();
				Class<?> inferredItemClass = ((DefaultTypeInfo) guessedIterationVariableType).getJavaType();
				if (!(inferredItemClass.isPrimitive() ? ClassUtils.primitiveToWrapperClass(inferredItemClass)
						: inferredItemClass).isAssignableFrom(
								(declaredItemClass.isPrimitive() ? ClassUtils.primitiveToWrapperClass(declaredItemClass)
										: declaredItemClass))) {
					throw new IllegalStateException("The declared iteration variable type '"
							+ declaredItemClass.getName() + "' is not derived from the detected item type '"
							+ inferredItemClass.getName() + "'");
				}
			}
		} catch (Exception e) {
			throw new ValidationError("Iteration variable type evaluation error", e);
		}
		InstantiationUtils.validateValue(getIterationListValue(), TypeInfoProvider.getTypeInfo(Object.class),
				listItemInitializerFacade, "iteration list value", true, variableDeclarations, solutionInstance);
	}

	@Override
	public String toString() {
		return preprendExpression(null);
	}

}