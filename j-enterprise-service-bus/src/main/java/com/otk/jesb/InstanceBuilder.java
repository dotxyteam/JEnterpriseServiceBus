package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ExecutionContext.Variable;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.Plan.ValidationContext.VariableDeclaration;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.iterable.map.MapEntryTypeInfoProxy;
import xy.reflect.ui.info.type.iterable.map.StandardMapEntry;
import xy.reflect.ui.info.type.iterable.map.StandardMapEntryTypeInfo;
import com.otk.jesb.util.Accessor;
import xy.reflect.ui.util.ReflectionUIUtils;

public class InstanceBuilder {

	private String typeName;
	private Accessor<String> dynamicTypeNameAccessor;
	private List<ParameterInitializer> parameterInitializers = new ArrayList<ParameterInitializer>();
	private List<FieldInitializer> fieldInitializers = new ArrayList<FieldInitializer>();
	private List<ListItemInitializer> listItemInitializers = new ArrayList<ListItemInitializer>();
	private String selectedConstructorSignature;

	public InstanceBuilder() {
	}

	public InstanceBuilder(String typeName) {
		this.typeName = typeName;
	}

	public InstanceBuilder(Accessor<String> typeNameAccessor) {
		this.dynamicTypeNameAccessor = typeNameAccessor;
	}

	public InstanceBuilderFacade getFacade() {
		return (InstanceBuilderFacade) getFacade(this, null);
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
		this.typeName = null;
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

	public List<ParameterInitializer> getParameterInitializers() {
		return parameterInitializers;
	}

	public void setParameterInitializers(List<ParameterInitializer> parameterInitializers) {
		this.parameterInitializers = parameterInitializers;
	}

	public List<FieldInitializer> getFieldInitializers() {
		return fieldInitializers;
	}

	public void setFieldInitializers(List<FieldInitializer> fieldInitializers) {
		this.fieldInitializers = fieldInitializers;
	}

	public List<ListItemInitializer> getListItemInitializers() {
		return listItemInitializers;
	}

	public void setListItemInitializers(List<ListItemInitializer> listItemInitializers) {
		this.listItemInitializers = listItemInitializers;
	}

	public Object build(EvaluationContext context) throws Exception {
		InstanceBuilderFacade instanceBuilderFacade = (InstanceBuilderFacade) getFacade(this,
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
			ParameterInitializer parameterInitializer = getParameterInitializer(parameterInfo.getPosition(),
					parameterInfo.getType().getName());
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
			for (ListItemInitializer listItemInitializer : listItemInitializers) {
				ListItemInitializerFacade listItemInitializerFacade = new ListItemInitializerFacade(
						instanceBuilderFacade, listItemInitializers.indexOf(listItemInitializer));
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
		return object;
	}

	public VerificationContext findFunctionVerificationContext(Function function, ValidationContext validationContext,
			Facade parentFacade) {
		InstanceBuilderFacade currentInstanceBuilderFacade = new InstanceBuilderFacade(parentFacade, this);
		for (ParameterInitializer parameterInitializer : parameterInitializers) {
			ParameterInitializerFacade currentFacade = new ParameterInitializerFacade(currentInstanceBuilderFacade,
					parameterInitializers.indexOf(parameterInitializer));
			if (parameterInitializer.getParameterValue() == function) {
				return new VerificationContext(validationContext, currentFacade);
			}
			if (parameterInitializer.getParameterValue() instanceof InstanceBuilder) {
				VerificationContext verificationContext = ((InstanceBuilder) parameterInitializer.getParameterValue())
						.findFunctionVerificationContext(function, validationContext, currentFacade);
				if (verificationContext != null) {
					return verificationContext;
				}
			}
		}
		for (FieldInitializer fieldInitializer : fieldInitializers) {
			FieldInitializerFacade currentFacade = new FieldInitializerFacade(currentInstanceBuilderFacade,
					fieldInitializer.getFieldName());
			if (fieldInitializer.getCondition() == function) {
				return new VerificationContext(validationContext, currentFacade);
			}
			if (fieldInitializer.getFieldValue() == function) {
				return new VerificationContext(validationContext, currentFacade);
			}
			if (fieldInitializer.getFieldValue() instanceof InstanceBuilder) {
				VerificationContext verificationContext = ((InstanceBuilder) fieldInitializer.getFieldValue())
						.findFunctionVerificationContext(function, validationContext, currentFacade);
				if (verificationContext != null) {
					return verificationContext;
				}
			}
		}
		for (ListItemInitializer listItemInitializer : listItemInitializers) {
			ListItemInitializerFacade currentFacade = new ListItemInitializerFacade(currentInstanceBuilderFacade,
					listItemInitializers.indexOf(listItemInitializer));
			if (listItemInitializer.getCondition() == function) {
				return new VerificationContext(validationContext, currentFacade);
			}
			VariableDeclaration iterationVariableDeclaration = null;
			int iterationVariableDeclarationPosition = -1;
			if (listItemInitializer.getItemReplication() != null) {
				if (listItemInitializer.getItemReplication().getIterationListValue() == function) {
					return new VerificationContext(validationContext, currentFacade);
				}
				if (listItemInitializer.getItemReplication().getIterationListValue() instanceof InstanceBuilder) {
					VerificationContext verificationContext = ((InstanceBuilder) listItemInitializer
							.getItemReplication().getIterationListValue()).findFunctionVerificationContext(function,
									validationContext, currentFacade);
					if (verificationContext != null) {
						return verificationContext;
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
				return new VerificationContext(iterationValidationContext, currentFacade);
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
				VerificationContext verificationContext = ((InstanceBuilder) listItemInitializer.getItemValue())
						.findFunctionVerificationContext(function, iterationValidationContext, currentFacade);
				if (verificationContext != null) {
					return verificationContext;
				}
			}
		}
		return null;
	}

	public ParameterInitializer getParameterInitializer(int parameterPosition, String parameterTypeName) {
		for (ParameterInitializer parameterInitializer : parameterInitializers) {
			if ((parameterInitializer.getParameterPosition() == parameterPosition)
					&& parameterTypeName.equals(parameterInitializer.getParameterTypeName())) {
				return parameterInitializer;
			}
		}
		return null;
	}

	public void removeParameterInitializer(int parameterPosition, String parameterTypeName) {
		for (Iterator<ParameterInitializer> it = parameterInitializers.iterator(); it.hasNext();) {
			ParameterInitializer parameterInitializer = it.next();
			if ((parameterInitializer.getParameterPosition() == parameterPosition)
					&& parameterTypeName.equals(parameterInitializer.getParameterTypeName())) {
				it.remove();
			}
		}
	}

	public FieldInitializer getFieldInitializer(String fieldName) {
		for (FieldInitializer fieldInitializer : fieldInitializers) {
			if (fieldInitializer.getFieldName().equals(fieldName)) {
				return fieldInitializer;
			}
		}
		return null;
	}

	public void removeFieldInitializer(String fieldName) {
		for (Iterator<FieldInitializer> it = fieldInitializers.iterator(); it.hasNext();) {
			FieldInitializer fieldInitializer = it.next();
			if (fieldInitializer.getFieldName().equals(fieldName)) {
				it.remove();
			}
		}
	}

	public static List<Facade> getAncestorFacades(Facade facade) {
		List<Facade> result = new ArrayList<InstanceBuilder.Facade>();
		if (facade instanceof InstanceBuilderFacade) {
			InstanceBuilderFacade specificFacade = (InstanceBuilderFacade) facade;
			if (specificFacade.getParent() != null) {
				result.add(specificFacade.getParent());
				result.addAll(getAncestorFacades(specificFacade.getParent()));
			}
		} else if (facade instanceof FieldInitializerFacade) {
			FieldInitializerFacade specificFacade = (FieldInitializerFacade) facade;
			InstanceBuilderFacade facadeParent = specificFacade.getParent();
			result.add(facadeParent);
			result.addAll(getAncestorFacades(facadeParent));
		} else if (facade instanceof ParameterInitializerFacade) {
			ParameterInitializerFacade specificFacade = (ParameterInitializerFacade) facade;
			InstanceBuilderFacade facadeParent = specificFacade.getParent();
			result.add(facadeParent);
			result.addAll(getAncestorFacades(facadeParent));
		} else if (facade instanceof ListItemInitializerFacade) {
			ListItemInitializerFacade specificFacade = (ListItemInitializerFacade) facade;
			InstanceBuilderFacade facadeParent = specificFacade.getParent();
			result.add(facadeParent);
			result.addAll(getAncestorFacades(facadeParent));
		} else {
			throw new AssertionError();
		}
		return result;
	}

	public static Facade getFacade(Object node, Facade parentFacade) {
		if (node instanceof MapEntryBuilder) {
			return new MapEntryBuilderFacade(parentFacade, (MapEntryBuilder) node);
		} else if (node instanceof InstanceBuilder) {
			return new InstanceBuilderFacade(parentFacade, (InstanceBuilder) node);
		} else if (node instanceof FieldInitializer) {
			return new FieldInitializerFacade((InstanceBuilderFacade) parentFacade,
					((FieldInitializer) node).getFieldName());
		} else if (node instanceof ParameterInitializer) {
			return new ParameterInitializerFacade((InstanceBuilderFacade) parentFacade,
					((ParameterInitializer) node).getParameterPosition());
		} else if (node instanceof ListItemInitializer) {
			return new ListItemInitializerFacade((InstanceBuilderFacade) parentFacade,
					((InstanceBuilderFacade) parentFacade).getUnderlying().getListItemInitializers()
							.indexOf((ListItemInitializer) node));
		} else {
			throw new AssertionError();
		}
	}

	@Override
	public String toString() {
		return "<" + getTypeName() + ">";
	}

	public static class MapEntryBuilder extends InstanceBuilder {

		public MapEntryBuilder() {
			super(StandardMapEntry.class.getName());
		}

	}

	public static class ParameterInitializer {

		private int parameterPosition;
		private String parameterTypeName;
		private Object parameterValue;

		public ParameterInitializer() {
		}

		public ParameterInitializer(int parameterPosition, String parameterTypeName, Object parameterValue) {
			super();
			this.parameterPosition = parameterPosition;
			this.parameterTypeName = parameterTypeName;
			this.parameterValue = parameterValue;
		}

		public int getParameterPosition() {
			return parameterPosition;
		}

		public void setParameterPosition(int parameterPosition) {
			this.parameterPosition = parameterPosition;
		}

		public String getParameterTypeName() {
			return parameterTypeName;
		}

		public void setParameterTypeName(String parameterTypeName) {
			this.parameterTypeName = parameterTypeName;
		}

		public Object getParameterValue() {
			return parameterValue;
		}

		public void setParameterValue(Object parameterValue) {
			this.parameterValue = parameterValue;
		}

	}

	public static class FieldInitializer {

		private String fieldName;
		private Object fieldValue;
		private Function condition;

		public FieldInitializer() {
		}

		public FieldInitializer(String fieldName, Object fieldValue) {
			this.fieldName = fieldName;
			this.fieldValue = fieldValue;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public Function getCondition() {
			return condition;
		}

		public void setCondition(Function condition) {
			this.condition = condition;
		}

		public Object getFieldValue() {
			return fieldValue;
		}

		public void setFieldValue(Object fieldValue) {
			this.fieldValue = fieldValue;
		}

	}

	public static class ListItemInitializer {

		private Object itemValue;
		private ListItemReplication itemReplication;
		private Function condition;

		public ListItemInitializer() {
		}

		public ListItemInitializer(Object itemValue) {
			this.itemValue = itemValue;
		}

		public Object getItemValue() {
			return itemValue;
		}

		public void setItemValue(Object itemValue) {
			this.itemValue = itemValue;
		}

		public Function getCondition() {
			return condition;
		}

		public void setCondition(Function condition) {
			this.condition = condition;
		}

		public ListItemReplication getItemReplication() {
			return itemReplication;
		}

		public void setItemReplication(ListItemReplication itemReplication) {
			this.itemReplication = itemReplication;
		}

	}

	public static class ListItemReplication {

		private String iterationVariableName = "current";
		private Object iterationListValue = new ArrayList<Object>();

		public String getIterationVariableName() {
			return iterationVariableName;
		}

		public void setIterationVariableName(String iterationVariableName) {
			this.iterationVariableName = iterationVariableName;
		}

		public Object getIterationListValue() {
			return iterationListValue;
		}

		public void setIterationListValue(Object iterationListValue) {
			this.iterationListValue = iterationListValue;
		}

		public static class IterationVariable implements Variable {

			private ListItemReplication itemReplication;
			private Object iterationVariableValue;

			public IterationVariable(ListItemReplication itemReplication, Object iterationVariableValue) {
				this.itemReplication = itemReplication;
				this.iterationVariableValue = iterationVariableValue;
			}

			@Override
			public Object getValue() {
				return iterationVariableValue;
			}

			@Override
			public String getName() {
				return itemReplication.getIterationVariableName();
			}

		}

	}

	public static class Function {
		private String functionBody;

		public Function() {
		}

		public Function(String functionBody) {
			this.functionBody = functionBody;
		}

		public String getFunctionBody() {
			return functionBody;
		}

		public void setFunctionBody(String functionBody) {
			this.functionBody = functionBody;
		}

	}

	public static class EnumerationItemSelector {

		private List<String> itemNames;
		private String selectedItemName;

		public EnumerationItemSelector() {
		}

		public EnumerationItemSelector(List<String> itemNames) {
			this.itemNames = itemNames;
			selectedItemName = itemNames.get(0);
		}

		public List<String> getItemNames() {
			return itemNames;
		}

		public String getSelectedItemName() {
			return selectedItemName;
		}

		public void setSelectedItemName(String selectedItemName) {
			this.selectedItemName = selectedItemName;
		}

		public void configure(IEnumerationTypeInfo enumType) {
			itemNames = Arrays.asList(enumType.getValues()).stream().map(item -> enumType.getValueInfo(item).getName())
					.collect(Collectors.toList());
		}

	}

	public static interface Facade {

		List<Facade> getChildren();

		boolean isConcrete();

		void setConcrete(boolean b);

		Object getUnderlying();

	}

	public static class InstanceBuilderFacade implements Facade {

		private Facade parent;
		private InstanceBuilder underlying;

		public InstanceBuilderFacade(Facade parent, InstanceBuilder underlying) {
			this.parent = parent;
			this.underlying = underlying;
		}

		public Facade getParent() {
			return parent;
		}

		@Override
		public boolean isConcrete() {
			if (parent != null) {
				return parent.isConcrete();
			}
			return true;
		}

		@Override
		public void setConcrete(boolean b) {
			if (parent != null) {
				parent.setConcrete(b);
			}
		}

		public String getTypeName() {
			return underlying.getTypeName();
		}

		public void setTypeName(String typeName) {
			underlying.setTypeName(typeName);
		}

		public String getSelectedConstructorSignature() {
			return underlying.getSelectedConstructorSignature();
		}

		public void setSelectedConstructorSignature(String selectedConstructorSignature) {
			underlying.setSelectedConstructorSignature(selectedConstructorSignature);
		}

		public List<String> getConstructorSignatureChoices() {
			List<String> result = new ArrayList<String>();
			ITypeInfo typeInfo = getTypeInfo();
			for (IMethodInfo constructor : typeInfo.getConstructors()) {
				result.add(constructor.getSignature());
			}
			return result;
		}

		@Override
		public InstanceBuilder getUnderlying() {
			return underlying;
		}

		public ITypeInfo getTypeInfo() {
			String actualTypeName = underlying
					.computeActualTypeName(MiscUtils.getAncestorStructureInstanceBuilders(parent));
			ITypeInfo result = TypeInfoProvider.getTypeInfo(actualTypeName);
			if (result instanceof IListTypeInfo) {
				if (parent instanceof FieldInitializerFacade) {
					FieldInitializerFacade listFieldInitializerFacade = (FieldInitializerFacade) parent;
					IFieldInfo listFieldInfo = listFieldInitializerFacade.getFieldInfo();
					result = TypeInfoProvider.getTypeInfo(actualTypeName, listFieldInfo);
				}
			}
			if (result instanceof IMapEntryTypeInfo) {
				if (parent instanceof ListItemInitializerFacade) {
					ListItemInitializerFacade listItemInitializerFacade = (ListItemInitializerFacade) parent;
					result = (StandardMapEntryTypeInfo) listItemInitializerFacade.getItemType();
				}
			}
			return result;
		}

		@Override
		public List<Facade> getChildren() {
			List<Facade> result = new ArrayList<InstanceBuilder.Facade>();
			ITypeInfo typeInfo = getTypeInfo();
			IMethodInfo constructor = MiscUtils.getConstructorInfo(typeInfo,
					underlying.getSelectedConstructorSignature());
			if (constructor != null) {
				for (IParameterInfo parameterInfo : constructor.getParameters()) {
					result.add(new ParameterInitializerFacade(this, parameterInfo.getPosition()));
				}
			}
			if (typeInfo instanceof IListTypeInfo) {
				int i = 0;
				for (; i < underlying.getListItemInitializers().size();) {
					result.add(new ListItemInitializerFacade(this, i));
					i++;
				}
				result.add(new ListItemInitializerFacade(this, i));
			} else {
				for (IFieldInfo field : typeInfo.getFields()) {
					if (field.isGetOnly()) {
						continue;
					}
					result.add(new FieldInitializerFacade(this, field.getName()));
				}
			}
			Collections.sort(result, new Comparator<Facade>() {
				List<Class<?>> CLASSES_ORDER = Arrays.asList(ParameterInitializerFacade.class,
						FieldInitializerFacade.class, ListItemInitializerFacade.class);

				@Override
				public int compare(Facade o1, Facade o2) {
					if (!o1.getClass().equals(o2.getClass())) {
						return Integer.valueOf(CLASSES_ORDER.indexOf(o1.getClass()))
								.compareTo(Integer.valueOf(CLASSES_ORDER.indexOf(o2.getClass())));
					}
					if ((o1 instanceof ParameterInitializerFacade) && (o2 instanceof ParameterInitializerFacade)) {
						ParameterInitializerFacade pif1 = (ParameterInitializerFacade) o1;
						ParameterInitializerFacade pif2 = (ParameterInitializerFacade) o2;
						return Integer.valueOf(pif1.getParameterPosition())
								.compareTo(Integer.valueOf(pif2.getParameterPosition()));
					} else if ((o1 instanceof FieldInitializerFacade) && (o2 instanceof FieldInitializerFacade)) {
						FieldInitializerFacade fif1 = (FieldInitializerFacade) o1;
						FieldInitializerFacade fif2 = (FieldInitializerFacade) o2;
						return fif1.getFieldInfo().getName().compareTo(fif2.getFieldInfo().getName());
					} else if ((o1 instanceof ListItemInitializerFacade) && (o2 instanceof ListItemInitializerFacade)) {
						ListItemInitializerFacade liif1 = (ListItemInitializerFacade) o1;
						ListItemInitializerFacade liif2 = (ListItemInitializerFacade) o2;
						return Integer.valueOf(liif1.getIndex()).compareTo(Integer.valueOf(liif2.getIndex()));
					} else {
						throw new AssertionError();
					}

				}
			});
			return result;
		}

		@Override
		public String toString() {
			return underlying.toString();
		}

	}

	public static class MapEntryBuilderFacade extends InstanceBuilderFacade {

		public MapEntryBuilderFacade(Facade parent, MapEntryBuilder mapEntryBuilder) {
			super(parent, mapEntryBuilder);
		}

		@Override
		public void setTypeName(String typeName) {
			throw new UnsupportedOperationException("Cannot change map entry type name");
		}

		@Override
		public MapEntryBuilder getUnderlying() {
			return (MapEntryBuilder) super.getUnderlying();
		}

		@Override
		public IMapEntryTypeInfo getTypeInfo() {
			return new MapEntryTypeInfoProxy((IMapEntryTypeInfo) super.getTypeInfo()) {

				@Override
				public List<IFieldInfo> getFields() {
					return Collections.emptyList();
				}

			};
		}

		@Override
		public String toString() {
			return "<MapEntry>";
		}

	}

	public enum ValueMode {
		PLAIN, FUNCTION
	}

	public static class ParameterInitializerFacade implements Facade {

		private InstanceBuilderFacade parent;
		private int parameterPosition;

		public ParameterInitializerFacade(InstanceBuilderFacade parent, int parameterPosition) {
			this.parent = parent;
			this.parameterPosition = parameterPosition;
		}

		public InstanceBuilderFacade getParent() {
			return parent;
		}

		public IParameterInfo getParameterInfo() {
			ITypeInfo parentTypeInfo = parent.getTypeInfo();
			IMethodInfo constructor = MiscUtils.getConstructorInfo(parentTypeInfo,
					parent.getUnderlying().getSelectedConstructorSignature());
			if (constructor == null) {
				throw new AssertionError();
			}
			return constructor.getParameters().get(parameterPosition);
		}

		@Override
		public ParameterInitializer getUnderlying() {
			IParameterInfo parameter = getParameterInfo();
			ParameterInitializer result = parent.getUnderlying().getParameterInitializer(parameterPosition,
					parameter.getType().getName());
			if (result == null) {
				result = new ParameterInitializer(parameterPosition, parameter.getType().getName(),
						MiscUtils.getDefaultInterpretableValue(parameter.getType(), this));
				parent.getUnderlying().getParameterInitializers().add(result);
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
			List<Facade> result = new ArrayList<InstanceBuilder.Facade>();
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

	public static class FieldInitializerFacade implements Facade {

		private InstanceBuilderFacade parent;
		private String fieldName;
		private Object fieldValue;

		public FieldInitializerFacade(InstanceBuilderFacade parent, String fieldName) {
			this.parent = parent;
			this.fieldName = fieldName;
			FieldInitializer fieldInitializer = getUnderlying();
			if (fieldInitializer == null) {
				this.fieldValue = createDefaultFieldValue();
			} else {
				this.fieldValue = fieldInitializer.getFieldValue();
			}
		}

		public InstanceBuilderFacade getParent() {
			return parent;
		}

		private Object createDefaultFieldValue() {
			IFieldInfo field = getFieldInfo();
			return MiscUtils.getDefaultInterpretableValue(field.getType(), this);
		}

		public IFieldInfo getFieldInfo() {
			ITypeInfo parentTypeInfo = parent.getTypeInfo();
			return ReflectionUIUtils.findInfoByName(parentTypeInfo.getFields(), fieldName);
		}

		@Override
		public FieldInitializer getUnderlying() {
			return parent.getUnderlying().getFieldInitializer(fieldName);
		}

		public String getFieldName() {
			return fieldName;
		}

		public String getFieldTypeName() {
			return MiscUtils.makeTypeNamesRelative(getFieldInfo().getType().getName(),
					MiscUtils.getAncestorStructureInstanceBuilders(this));
		}

		public Function getCondition() {
			FieldInitializer fieldInitializer = getUnderlying();
			if (fieldInitializer == null) {
				return null;
			}
			return fieldInitializer.getCondition();
		}

		public void setCondition(Function condition) {
			setConcrete(true);
			FieldInitializer fieldInitializer = getUnderlying();
			if ((condition != null) && (condition.getFunctionBody() == null)) {
				condition = new Function("return true;");
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
				parent.getUnderlying().getFieldInitializers().add(new FieldInitializer(fieldName, fieldValue));
			} else {
				parent.getUnderlying().removeFieldInitializer(fieldName);
				fieldValue = createDefaultFieldValue();
			}
		}

		public ValueMode getFieldValueMode() {
			FieldInitializer fieldInitializer = getUnderlying();
			if (fieldInitializer == null) {
				return null;
			}
			return MiscUtils.getValueMode(fieldInitializer.getFieldValue());
		}

		public void setFieldValueMode(ValueMode valueMode) {
			setConcrete(true);
			if (valueMode == getFieldValueMode()) {
				return;
			}
			IFieldInfo field = getFieldInfo();
			Object newFieldValue = MiscUtils.getDefaultInterpretableValue(field.getType(), valueMode, this);
			setFieldValue(newFieldValue);
		}

		public Object getFieldValue() {
			FieldInitializer fieldInitializer = getUnderlying();
			if (fieldInitializer == null) {
				return null;
			}
			return MiscUtils.maintainInterpretableValue(fieldInitializer.getFieldValue(), getFieldInfo().getType());
		}

		public void setFieldValue(Object value) {
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
			List<Facade> result = new ArrayList<InstanceBuilder.Facade>();
			if (fieldValue instanceof InstanceBuilder) {
				result.add(new InstanceBuilderFacade(this, (InstanceBuilder) fieldValue));
			}
			return result;
		}

		@Override
		public String toString() {
			return fieldName + ((getCondition() != null) ? "?" : "");
		}

	}

	public static class ListItemInitializerFacade implements Facade {

		private InstanceBuilderFacade parent;
		private int index;
		private Object itemValue;

		public ListItemInitializerFacade(InstanceBuilderFacade parent, int index) {
			this.parent = parent;
			this.index = index;
			ListItemInitializer listItemInitializer = getUnderlying();
			if (listItemInitializer == null) {
				this.itemValue = createDefaultItemValue();
			} else {
				this.itemValue = listItemInitializer.getItemValue();
			}
		}

		public InstanceBuilderFacade getParent() {
			return parent;
		}

		public int getIndex() {
			return index;
		}

		public ListItemReplicationFacade getItemReplicationFacade() {
			ListItemInitializer listItemInitializer = getUnderlying();
			if (listItemInitializer == null) {
				return null;
			}
			return (listItemInitializer.getItemReplication() == null) ? null
					: new ListItemReplicationFacade(listItemInitializer.getItemReplication());
		}

		public void setItemReplicationFacade(ListItemReplicationFacade itemReplicationFacade) {
			setConcrete(true);
			ListItemInitializer listItemInitializer = getUnderlying();
			listItemInitializer
					.setItemReplication((itemReplicationFacade == null) ? null : itemReplicationFacade.getUnderlying());
		}

		public Function getCondition() {
			ListItemInitializer listItemInitializer = getUnderlying();
			if (listItemInitializer == null) {
				return null;
			}
			return listItemInitializer.getCondition();
		}

		public void setCondition(Function condition) {
			setConcrete(true);
			ListItemInitializer listItemInitializer = getUnderlying();
			if ((condition != null) && (condition.getFunctionBody() == null)) {
				condition = new Function("return true;");
			}
			listItemInitializer.setCondition(condition);
		}

		public Object getItemValue() {
			ListItemInitializer listItemInitializer = getUnderlying();
			if (listItemInitializer == null) {
				return null;
			}
			return MiscUtils.maintainInterpretableValue(listItemInitializer.getItemValue(), getItemType());
		}

		public void setItemValue(Object value) {
			setConcrete(true);
			ListItemInitializer listItemInitializer = getUnderlying();
			ITypeInfo itemType = getItemType();
			if ((value == null) && (itemType != null) && (itemType.isPrimitive())) {
				throw new AssertionError("Cannot add null item to primitive item list");
			}
			listItemInitializer.setItemValue(value);
		}

		public ValueMode getItemValueMode() {
			ListItemInitializer listItemInitializer = getUnderlying();
			if (listItemInitializer == null) {
				return null;
			}
			return MiscUtils.getValueMode(listItemInitializer.getItemValue());
		}

		public void setItemValueMode(ValueMode valueMode) {
			setConcrete(true);
			ListItemInitializer listItemInitializer = getUnderlying();
			if (valueMode == getItemValueMode()) {
				return;
			}
			ITypeInfo itemType = getItemType();
			itemValue = MiscUtils.getDefaultInterpretableValue(itemType, valueMode, this);
			listItemInitializer.setItemValue(itemValue);
		}

		private Object createDefaultItemValue() {
			ITypeInfo itemType = getItemType();
			return MiscUtils.getDefaultInterpretableValue(itemType, this);
		}

		public ITypeInfo getItemType() {
			ITypeInfo parentTypeInfo = parent.getTypeInfo();
			return ((IListTypeInfo) parentTypeInfo).getItemType();
		}

		public String getItemTypeName() {
			ITypeInfo itemType = getItemType();
			String result = (itemType == null) ? Object.class.getName() : itemType.getName();
			result = MiscUtils.makeTypeNamesRelative(result, MiscUtils.getAncestorStructureInstanceBuilders(this));
			return result;
		}

		@Override
		public ListItemInitializer getUnderlying() {
			if (index >= parent.getUnderlying().getListItemInitializers().size()) {
				return null;
			}
			return parent.getUnderlying().getListItemInitializers().get(index);
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
				parent.getUnderlying().getListItemInitializers().add(index, new ListItemInitializer(itemValue));
			} else {
				parent.getUnderlying().getListItemInitializers().remove(index);
				itemValue = createDefaultItemValue();
			}
		}

		@Override
		public List<Facade> getChildren() {
			List<Facade> result = new ArrayList<InstanceBuilder.Facade>();
			if (itemValue instanceof MapEntryBuilder) {
				result.add(new MapEntryBuilderFacade(this, (MapEntryBuilder) itemValue));
			} else if (itemValue instanceof InstanceBuilder) {
				result.add(new InstanceBuilderFacade(this, (InstanceBuilder) itemValue));
			}
			return result;
		}

		@Override
		public String toString() {
			return "[" + index + "]" + ((getItemReplicationFacade() != null) ? "*" : "")
					+ ((getCondition() != null) ? "?" : "");
		}

	}

	public static class ListItemReplicationFacade {

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
			return MiscUtils.maintainInterpretableValue(listItemReplication.getIterationListValue(),
					TypeInfoProvider.getTypeInfo(Object.class.getName()));
		}

		public void setIterationListValue(Object iterationListValue) {
			listItemReplication.setIterationListValue(iterationListValue);
		}

		public ValueMode getIterationListValueMode() {
			return MiscUtils.getValueMode(listItemReplication.getIterationListValue());
		}

		public void setIterationListValueMode(ValueMode valueMode) {
			Object iterationListValue;
			if (valueMode == ValueMode.FUNCTION) {
				String functionBody = "return new " + ArrayList.class.getName() + "<Object>();";
				iterationListValue = new Function(functionBody);
			} else {
				iterationListValue = new ArrayList<Object>();
			}
			listItemReplication.setIterationListValue(iterationListValue);
		}
	}

	public static class EvaluationContext {

		private ExecutionContext executionContext;
		private Facade currentFacade;

		public EvaluationContext(ExecutionContext executionContext, Facade currentFacade) {
			this.executionContext = executionContext;
			this.currentFacade = currentFacade;
		}

		public ExecutionContext getExecutionContext() {
			return executionContext;
		}

		public Facade getCurrentFacade() {
			return currentFacade;
		}

	}

	public static class VerificationContext {
		private ValidationContext validationContext;
		private Facade currentFacade;

		public VerificationContext(ValidationContext validationContext, Facade currentFacade) {
			this.validationContext = validationContext;
			this.currentFacade = currentFacade;
		}

		public ValidationContext getValidationContext() {
			return validationContext;
		}

		public Facade getCurrentFacade() {
			return currentFacade;
		}

	}

}
