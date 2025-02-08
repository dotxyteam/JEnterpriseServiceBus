package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.Plan;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.Plan.ValidationContext.VariableDeclaration;
import com.otk.jesb.instantiation.Function.CompilationContext;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

import com.otk.jesb.util.Accessor;

public class InstanceBuilder extends InitializationCase{

	private String typeName;
	private Accessor<String> dynamicTypeNameAccessor;
	private String selectedConstructorSignature;
	
	public InstanceBuilder() {
	}

	public InstanceBuilder(String typeName) {
		this.typeName = typeName;
	}

	public InstanceBuilder(Accessor<String> dynamicTypeNameAccessor) {
		this.dynamicTypeNameAccessor = dynamicTypeNameAccessor;
	}

	public String getTypeName() {
		if (dynamicTypeNameAccessor != null) {
			return "<Dynamic>";
		}
		return typeName;
	}

	public void setTypeName(String typeName) {
		if (dynamicTypeNameAccessor != null) {
			if ("<Dynamic>".equals(typeName)) {
				return;
			}
			throw new UnsupportedOperationException();
		}
		this.typeName = typeName;
	}

	public Accessor<String> getDynamicTypeNameAccessor() {
		return dynamicTypeNameAccessor;
	}

	public void setDynamicTypeNameAccessor(Accessor<String> dynamicTypeNameAccessor) {
		this.dynamicTypeNameAccessor = dynamicTypeNameAccessor;
		if (dynamicTypeNameAccessor != null) {
			this.typeName = null;
		}
	}

	public String computeActualTypeName(List<InstanceBuilder> ancestorStructureInstanceBuilders) {
		String result;
		if (dynamicTypeNameAccessor != null) {
			result = dynamicTypeNameAccessor.get();
		} else {
			result = typeName;
		}
		result = MiscUtils.makeTypeNamesAbsolute(result, ancestorStructureInstanceBuilders);
		return result;
	}

	public String getSelectedConstructorSignature() {
		return selectedConstructorSignature;
	}

	public void setSelectedConstructorSignature(String selectedConstructorSignature) {
		this.selectedConstructorSignature = selectedConstructorSignature;
	}

	

	public Object build(EvaluationContext context) throws Exception {
		InstanceBuilderFacade instanceBuilderFacade = (InstanceBuilderFacade) Facade.get(this,
				context.getCurrentFacade());
		ITypeInfo typeInfo = instanceBuilderFacade.getTypeInfo();
		IMethodInfo constructor = MiscUtils.getConstructorInfo(typeInfo, selectedConstructorSignature);
		if (constructor == null) {
			String actualTypeName = computeActualTypeName(
					MiscUtils.getAncestorStructureInstanceBuilders(context.getCurrentFacade()));
			if (selectedConstructorSignature == null) {
				throw new AssertionError("Cannot create '" + actualTypeName + "' instance: No constructor available");
			} else {
				throw new AssertionError("Cannot create '" + actualTypeName + "' instance: Constructor not found: '"
						+ selectedConstructorSignature + "'");
			}
		}
		Object[] parameterValues = new Object[constructor.getParameters().size()];
		for (IParameterInfo parameterInfo : constructor.getParameters()) {
			ParameterInitializer parameterInitializer = getDynamicParameterInitializer(context,
					parameterInfo.getPosition(), parameterInfo.getType().getName());
			ParameterInitializerFacade parameterInitializerFacade = new ParameterInitializerFacade(
					instanceBuilderFacade, parameterInfo.getPosition());
			Object parameterValue;
			if (parameterInitializer == null) {
				parameterValue = MiscUtils.interpretValue(
						MiscUtils.getDefaultInterpretableValue(parameterInfo.getType(), context.getCurrentFacade()),
						parameterInfo.getType(),
						new EvaluationContext(context.getExecutionContext(), parameterInitializerFacade));
			} else {
				parameterValue = MiscUtils.interpretValue(parameterInitializer.getParameterValue(),
						parameterInfo.getType(),
						new EvaluationContext(context.getExecutionContext(), parameterInitializerFacade));
			}
			parameterValues[parameterInfo.getPosition()] = parameterValue;
		}
		Object object;
		if (typeInfo instanceof IListTypeInfo) {
			IListTypeInfo listTypeInfo = (IListTypeInfo) typeInfo;
			List<Object> itemList = new ArrayList<Object>();
			for (ListItemInitializer listItemInitializer : getListItemInitializers()) {
				ListItemInitializerFacade listItemInitializerFacade = new ListItemInitializerFacade(
						instanceBuilderFacade, getListItemInitializers().indexOf(listItemInitializer));
				if (!MiscUtils.isConditionFullfilled(listItemInitializer.getCondition(),
						new EvaluationContext(context.getExecutionContext(), listItemInitializerFacade))) {
					continue;
				}
				ListItemReplication itemReplication = listItemInitializer.getItemReplication();
				if (itemReplication != null) {
					Object iterationListValue = MiscUtils.interpretValue(itemReplication.getIterationListValue(),
							TypeInfoProvider.getTypeInfo(Object.class.getName()),
							new EvaluationContext(context.getExecutionContext(), listItemInitializerFacade));
					if (iterationListValue == null) {
						throw new AssertionError("Cannot replicate item: Iteration list value is null");
					}
					ITypeInfo iterationListTypeInfo = TypeInfoProvider
							.getTypeInfo(iterationListValue.getClass().getName());
					if (!(iterationListTypeInfo instanceof IListTypeInfo)) {
						throw new AssertionError("Cannot replicate item: Iteration list value is not iterable: '"
								+ iterationListValue + "'");
					}
					Object[] iterationListArray = ((IListTypeInfo) iterationListTypeInfo).toArray(iterationListValue);
					for (Object iterationVariableValue : iterationListArray) {
						EvaluationContext iterationContext = new EvaluationContext(new Plan.ExecutionContext(
								context.getExecutionContext(),
								new ListItemReplication.IterationVariable(itemReplication, iterationVariableValue)),
								context.getCurrentFacade());
						Object itemValue = MiscUtils.interpretValue(listItemInitializer.getItemValue(),
								(listTypeInfo.getItemType() != null) ? listTypeInfo.getItemType()
										: TypeInfoProvider.getTypeInfo(Object.class.getName()),
								new EvaluationContext(iterationContext.getExecutionContext(),
										listItemInitializerFacade));
						itemList.add(itemValue);
					}
				} else {
					Object itemValue = MiscUtils.interpretValue(listItemInitializer.getItemValue(),
							(listTypeInfo.getItemType() != null) ? listTypeInfo.getItemType()
									: TypeInfoProvider.getTypeInfo(Object.class.getName()),
							new EvaluationContext(context.getExecutionContext(), listItemInitializerFacade));
					itemList.add(itemValue);
				}
			}
			if (listTypeInfo.canReplaceContent()) {
				object = constructor.invoke(null, new InvocationData(null, constructor, parameterValues));
				listTypeInfo.replaceContent(object, itemList.toArray());
			} else if (listTypeInfo.canInstantiateFromArray()) {
				object = listTypeInfo.fromArray(itemList.toArray());
			} else {
				throw new AssertionError("Cannot initialize list of type " + listTypeInfo
						+ ": Cannot replace instance content or instantiate from array");
			}
		} else {
			object = constructor.invoke(null, new InvocationData(null, constructor, parameterValues));
		}
		for (IFieldInfo field : typeInfo.getFields()) {
			if (field.isGetOnly()) {
				continue;
			}
			FieldInitializer fieldInitializer = getFieldInitializer(field.getName());
			if (fieldInitializer == null) {
				continue;
			}
			if (!MiscUtils.isConditionFullfilled(fieldInitializer.getCondition(), context)) {
				continue;
			}
			FieldInitializerFacade fieldInitializerFacade = new FieldInitializerFacade(instanceBuilderFacade,
					fieldInitializer.getFieldName());
			Object fieldValue = MiscUtils.interpretValue(fieldInitializer.getFieldValue(), field.getType(),
					new EvaluationContext(context.getExecutionContext(), fieldInitializerFacade));
			field.setValue(object, fieldValue);
		}
		if (object instanceof NullInstance) {
			return null;
		}
		return object;
	}

	

	public CompilationContext findFunctionCompilationContext(Function function, ValidationContext validationContext,
			Facade parentFacade) {
		InstanceBuilderFacade currentInstanceBuilderFacade = new InstanceBuilderFacade(parentFacade, this);
		for (ParameterInitializer parameterInitializer : getParameterInitializers()) {
			ParameterInitializerFacade currentFacade = new ParameterInitializerFacade(currentInstanceBuilderFacade,
					getParameterInitializers().indexOf(parameterInitializer));
			if (parameterInitializer.getParameterValue() == function) {
				return new CompilationContext(new VerificationContext(validationContext, currentFacade),
						((DefaultTypeInfo) currentFacade.getParameterInfo().getType()).getJavaType());
			}
			if (parameterInitializer.getParameterValue() instanceof InstanceBuilder) {
				CompilationContext compilationContext = ((InstanceBuilder) parameterInitializer.getParameterValue())
						.findFunctionCompilationContext(function, validationContext, currentFacade);
				if (compilationContext != null) {
					return compilationContext;
				}
			}
		}
		for (FieldInitializer fieldInitializer : getFieldInitializers()) {
			FieldInitializerFacade currentFacade = new FieldInitializerFacade(currentInstanceBuilderFacade,
					fieldInitializer.getFieldName());
			if (fieldInitializer.getCondition() == function) {
				return new CompilationContext(new VerificationContext(validationContext, currentFacade), boolean.class);
			}
			if (fieldInitializer.getFieldValue() == function) {
				return new CompilationContext(new VerificationContext(validationContext, currentFacade),
						((DefaultTypeInfo) currentFacade.getFieldInfo().getType()).getJavaType());
			}
			if (fieldInitializer.getFieldValue() instanceof InstanceBuilder) {
				CompilationContext compilationContext = ((InstanceBuilder) fieldInitializer.getFieldValue())
						.findFunctionCompilationContext(function, validationContext, currentFacade);
				if (compilationContext != null) {
					return compilationContext;
				}
			}
		}
		for (ListItemInitializer listItemInitializer : getListItemInitializers()) {
			ListItemInitializerFacade currentFacade = new ListItemInitializerFacade(currentInstanceBuilderFacade,
					getListItemInitializers().indexOf(listItemInitializer));
			if (listItemInitializer.getCondition() == function) {
				return new CompilationContext(new VerificationContext(validationContext, currentFacade), boolean.class);
			}
			VariableDeclaration iterationVariableDeclaration = null;
			int iterationVariableDeclarationPosition = -1;
			if (listItemInitializer.getItemReplication() != null) {
				if (listItemInitializer.getItemReplication().getIterationListValue() == function) {
					return new CompilationContext(new VerificationContext(validationContext, currentFacade),
							Object.class);
				}
				if (listItemInitializer.getItemReplication().getIterationListValue() instanceof InstanceBuilder) {
					CompilationContext compilationContext = ((InstanceBuilder) listItemInitializer.getItemReplication()
							.getIterationListValue()).findFunctionCompilationContext(function, validationContext,
									currentFacade);
					if (compilationContext != null) {
						return compilationContext;
					}
				}
				iterationVariableDeclaration = new Plan.ValidationContext.VariableDeclaration() {

					@Override
					public String getVariableName() {
						return listItemInitializer.getItemReplication().getIterationVariableName();
					}

					@Override
					public Class<?> getVariableClass() {
						return Object.class;
					}
				};
				iterationVariableDeclarationPosition = validationContext.getVariableDeclarations().size();
			}
			if (listItemInitializer.getItemValue() == function) {
				ValidationContext iterationValidationContext = validationContext;
				if (iterationVariableDeclaration != null) {
					List<VariableDeclaration> newVariableDeclarations = new ArrayList<Plan.ValidationContext.VariableDeclaration>(
							validationContext.getVariableDeclarations());
					newVariableDeclarations.add(iterationVariableDeclarationPosition, iterationVariableDeclaration);
					iterationValidationContext = new ValidationContext(iterationValidationContext.getPlan(),
							newVariableDeclarations);
				}
				return new CompilationContext(new VerificationContext(iterationValidationContext, currentFacade),
						((DefaultTypeInfo) currentFacade.getItemType()).getJavaType());
			}
			if (listItemInitializer.getItemValue() instanceof InstanceBuilder) {
				ValidationContext iterationValidationContext = validationContext;
				if (iterationVariableDeclaration != null) {
					List<VariableDeclaration> newVariableDeclarations = new ArrayList<Plan.ValidationContext.VariableDeclaration>(
							validationContext.getVariableDeclarations());
					newVariableDeclarations.add(iterationVariableDeclarationPosition, iterationVariableDeclaration);
					iterationValidationContext = new ValidationContext(iterationValidationContext.getPlan(),
							newVariableDeclarations);
				}
				CompilationContext compilationContext = ((InstanceBuilder) listItemInitializer.getItemValue())
						.findFunctionCompilationContext(function, iterationValidationContext, currentFacade);
				if (compilationContext != null) {
					return compilationContext;
				}
			}
		}
		return null;
	}

	
	@Override
	public String toString() {
		return "<" + getTypeName() + ">";
	}

}
