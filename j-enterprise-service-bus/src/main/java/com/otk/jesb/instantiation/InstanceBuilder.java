package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.compiler.InMemoryCompiler;
import com.otk.jesb.meta.TypeInfoProvider;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.InstantiationUtils;

public class InstanceBuilder extends InitializationCase {

	private String typeName;
	private Accessor<String> dynamicTypeNameAccessor;
	private String selectedConstructorSignature;

	public InstanceBuilder() {
	}

	public InstanceBuilder(String typeName) {
		setTypeName(typeName);
	}

	public InstanceBuilder(Accessor<String> dynamicTypeNameAccessor) {
		setDynamicTypeNameAccessor(dynamicTypeNameAccessor);
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
			throw new UnexpectedError();
		}
		this.typeName = typeName;
		if (typeName != null) {
			if (!"<Dynamic>".equals(typeName)) {
				if (!typeName.contains(InstantiationUtils.RELATIVE_TYPE_NAME_VARIABLE_PART_REFRENCE)) {
					Class<?> clazz;
					try {
						clazz = TypeInfoProvider.getClass(typeName);
					} catch (Throwable t) {
						return;
					}
					if (clazz.getClassLoader() != null) {
						if (clazz.getClassLoader().getClass().getName().startsWith(InMemoryCompiler.class.getName())) {
							throw new UnexpectedError(
									"An instance builder absolute type name should not reference a dynamically generated class: "
											+ clazz.getName());
						}
					}
				}
			}
		}
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
		if (result == null) {
			result = NullInstance.class.getName();
		}
		result = InstantiationUtils.makeTypeNamesAbsolute(result, ancestorStructureInstanceBuilders);
		return result;
	}

	public String getSelectedConstructorSignature() {
		return selectedConstructorSignature;
	}

	public void setSelectedConstructorSignature(String selectedConstructorSignature) {
		this.selectedConstructorSignature = selectedConstructorSignature;
	}

	public Object build(InstantiationContext context) throws Exception {
		InstanceBuilderFacade instanceBuilderFacade = (InstanceBuilderFacade) Facade.get(this,
				context.getParentFacade());
		ITypeInfo typeInfo = instanceBuilderFacade.getTypeInfo();
		IMethodInfo constructor = InstantiationUtils.getConstructorInfo(typeInfo, selectedConstructorSignature);
		if (constructor == null) {
			String actualTypeName = computeActualTypeName(
					InstantiationUtils.getAncestorInstanceBuilders(context.getParentFacade()));
			if (selectedConstructorSignature == null) {
				throw new UnexpectedError("Cannot create '" + actualTypeName + "' instance: No constructor available");
			} else {
				throw new UnexpectedError("Cannot create '" + actualTypeName + "' instance: Constructor not found: '"
						+ selectedConstructorSignature + "'");
			}
		}
		List<Facade> initializerFacades = instanceBuilderFacade.collectLiveInitializerFacades(context);
		Object[] parameterValues = new Object[constructor.getParameters().size()];
		for (Facade facade : initializerFacades) {
			if (facade instanceof ParameterInitializerFacade) {
				ParameterInitializerFacade parameterInitializerFacade = (ParameterInitializerFacade) facade;
				Object parameterValue;
				parameterValue = InstantiationUtils.interpretValue(
						parameterInitializerFacade.getUnderlying().getParameterValue(),
						parameterInitializerFacade.getParameterInfo().getType(),
						new InstantiationContext(context, parameterInitializerFacade));
				parameterValues[parameterInitializerFacade.getParameterPosition()] = parameterValue;
			}
		}
		Object object;
		if (typeInfo instanceof IListTypeInfo) {
			IListTypeInfo listTypeInfo = (IListTypeInfo) typeInfo;
			List<Object> itemList = new ArrayList<Object>();
			List<ListItemInitializerFacade> listItemInitializerFacades = initializerFacades.stream()
					.filter(facade -> facade instanceof ListItemInitializerFacade)
					.map(facade -> (ListItemInitializerFacade) facade).collect(Collectors.toList());
			listItemInitializerFacades = new ArrayList<ListItemInitializerFacade>(listItemInitializerFacades);
			Collections.sort(listItemInitializerFacades, new Comparator<ListItemInitializerFacade>() {
				@Override
				public int compare(ListItemInitializerFacade o1, ListItemInitializerFacade o2) {
					return Integer.valueOf(o1.getIndex()).compareTo(Integer.valueOf(o2.getIndex()));
				}
			});
			for (ListItemInitializerFacade listItemInitializerFacade : listItemInitializerFacades) {
				if (!listItemInitializerFacade.isConcrete()) {
					continue;
				}
				if (!InstantiationUtils.isConditionFullfilled(listItemInitializerFacade.getCondition(),
						new InstantiationContext(context, listItemInitializerFacade))) {
					continue;
				}
				ListItemReplicationFacade itemReplicationFacade = listItemInitializerFacade.getItemReplicationFacade();
				if (itemReplicationFacade != null) {
					Object iterationListValue = InstantiationUtils.interpretValue(
							itemReplicationFacade.getIterationListValue(),
							TypeInfoProvider.getTypeInfo(Object.class.getName()),
							new InstantiationContext(context, listItemInitializerFacade));
					if (iterationListValue == null) {
						throw new UnexpectedError("Cannot replicate item: Iteration list value is null");
					}
					ITypeInfo actualIterationListType = TypeInfoProvider
							.getTypeInfo(iterationListValue.getClass().getName());
					if (!(actualIterationListType instanceof IListTypeInfo)) {
						throw new UnexpectedError("Cannot replicate item: Iteration list value is not iterable: '"
								+ iterationListValue + "'");
					}
					Object[] iterationListArray = ((IListTypeInfo) actualIterationListType).toArray(iterationListValue);
					ITypeInfo declaredIterationVariableType = itemReplicationFacade
							.getDeclaredIterationVariableTypeInfo();
					for (Object iterationVariableValue : iterationListArray) {
						if (declaredIterationVariableType != null) {
							if (!declaredIterationVariableType.supports(iterationVariableValue)) {
								throw new UnexpectedError("Cannot replicate item: Iteration variable value '"
										+ iterationVariableValue + "' is not compatible with the declared type '"
										+ declaredIterationVariableType.getName() + "'");
							}
						}
						ListItemReplication.IterationVariable iterationVariable = new ListItemReplication.IterationVariable(
								itemReplicationFacade.getUnderlying(), iterationVariableValue);
						Object itemValue = InstantiationUtils.interpretValue(
								listItemInitializerFacade.getUnderlying().getItemValue(),
								(listTypeInfo.getItemType() != null) ? listTypeInfo.getItemType()
										: TypeInfoProvider.getTypeInfo(Object.class.getName()),
								new InstantiationContext(new InstantiationContext(context, listItemInitializerFacade),
										iterationVariable));
						itemList.add(itemValue);
					}
				} else {
					Object itemValue = InstantiationUtils.interpretValue(
							listItemInitializerFacade.getUnderlying().getItemValue(),
							(listTypeInfo.getItemType() != null) ? listTypeInfo.getItemType()
									: TypeInfoProvider.getTypeInfo(Object.class.getName()),
							new InstantiationContext(context, listItemInitializerFacade));
					itemList.add(itemValue);
				}
			}
			if (listTypeInfo.canReplaceContent()) {
				object = constructor.invoke(null, new InvocationData(null, constructor, parameterValues));
				listTypeInfo.replaceContent(object, itemList.toArray());
			} else if (listTypeInfo.canInstantiateFromArray()) {
				object = listTypeInfo.fromArray(itemList.toArray());
			} else {
				throw new UnexpectedError("Cannot initialize list of type " + listTypeInfo
						+ ": Cannot replace instance content or instantiate from array");
			}
		} else {
			object = constructor.invoke(null, new InvocationData(null, constructor, parameterValues));
		}
		for (Facade facade : initializerFacades) {
			if (facade instanceof FieldInitializerFacade) {
				FieldInitializerFacade fieldInitializerFacade = (FieldInitializerFacade) facade;
				if (!InstantiationUtils.isConditionFullfilled(fieldInitializerFacade.getCondition(), context)) {
					continue;
				}
				IFieldInfo fieldInfo = fieldInitializerFacade.getFieldInfo();
				Object fieldValue = InstantiationUtils.interpretValue(
						fieldInitializerFacade.getUnderlying().getFieldValue(), fieldInfo.getType(),
						new InstantiationContext(context, fieldInitializerFacade));
				fieldInfo.setValue(object, fieldValue);
			}
		}
		if (object instanceof NullInstance) {
			return null;
		}
		return object;
	}

	@Override
	public String toString() {
		return "<" + getTypeName() + ">";
	}

}
