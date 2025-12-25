package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.PotentialError;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.solution.Solution;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.InstantiationUtils;

/**
 * Allows you to specify and execute the creation (instantiation) and simple
 * configuration of any object.
 * 
 * Reflection is used to get the created objects class from a final type name
 * computed with the provided {@link #typeName} or
 * {@link #dynamicTypeNameAccessor}.
 * 
 * This final type name may be relative in the sense that it could contain a
 * variable that will be resolved by using ancestor {@link InstanceBuilder}
 * instances.
 * 
 * If the {@link #selectedConstructorSignature} is not provided, then 1 of the
 * available constructors will be automatically selected.
 * 
 * @author olitank
 *
 */
public class InstanceBuilder extends InitializationCase {

	private String typeName;
	private Accessor<Solution, String> dynamicTypeNameAccessor;
	private String selectedConstructorSignature;

	/**
	 * The default constructor.
	 */
	public InstanceBuilder() {
	}

	/**
	 * Creates an instance that will have the provided type name.
	 * 
	 * @param typeName The class name of created objects.
	 */
	public InstanceBuilder(String typeName) {
		setTypeName(typeName);
	}

	/**
	 * Creates an instance that will use the provided accessor to get the type name.
	 * 
	 * @param dynamicTypeNameAccessor The accessor of created objects class name.
	 */
	public InstanceBuilder(Accessor<Solution, String> dynamicTypeNameAccessor) {
		setDynamicTypeNameAccessor(dynamicTypeNameAccessor);
	}

	/**
	 * @return The class name of created objects.
	 */
	public String getTypeName() {
		if (dynamicTypeNameAccessor != null) {
			return "<Dynamic>";
		}
		return typeName;
	}

	/**
	 * Updates the class name of created objects.
	 * 
	 * @param typeName The new class name of created objects.
	 */
	public void setTypeName(String typeName) {
		if (dynamicTypeNameAccessor != null) {
			if ("<Dynamic>".equals(typeName)) {
				return;
			}
			throw new UnexpectedError();
		}
		this.typeName = typeName;
	}

	/**
	 * @return The accessor of created objects class name.
	 */
	public Accessor<Solution, String> getDynamicTypeNameAccessor() {
		return dynamicTypeNameAccessor;
	}

	/**
	 * Updates the accessor of created objects class name.
	 * 
	 * @param dynamicTypeNameAccessor The new accessor of created objects class
	 *                                name.
	 */
	public void setDynamicTypeNameAccessor(Accessor<Solution, String> dynamicTypeNameAccessor) {
		this.dynamicTypeNameAccessor = dynamicTypeNameAccessor;
		if (dynamicTypeNameAccessor != null) {
			this.typeName = null;
		}
	}

	/**
	 * @param ancestorStructureInstanceBuilders The ancestor {@link InstanceBuilder}
	 * @param solutionInstance                  The current solution. instances.
	 * @return The created objects class name with any variable resolved using the
	 *         given ancestor {@link InstanceBuilder} instances.
	 */
	public String computeActualTypeName(List<InstanceBuilder> ancestorStructureInstanceBuilders,
			Solution solutionInstance) {
		String result;
		if (dynamicTypeNameAccessor != null) {
			result = dynamicTypeNameAccessor.get(solutionInstance);
		} else {
			if (RootInstanceBuilder.ROOT_INSTANCE_TYPE_NAME_REFERENCE.equals(typeName)) {
				result = RootInstanceBuilder.resolveRootInstanceTypeNameReference(ancestorStructureInstanceBuilders,
						solutionInstance);
			} else {
				result = typeName;
			}
		}
		if (result == null) {
			result = NullInstance.class.getName();
		}
		result = InstantiationUtils.makeTypeNamesAbsolute(result, ancestorStructureInstanceBuilders, solutionInstance);
		return result;
	}

	/**
	 * @return The signature of the constructor used to build the target objects, or
	 *         null.
	 */
	public String getSelectedConstructorSignature() {
		return selectedConstructorSignature;
	}

	/**
	 * Updates the signature of the constructor used to build the target objects.
	 * 
	 * @param selectedConstructorSignature The new signature or null.
	 */
	public void setSelectedConstructorSignature(String selectedConstructorSignature) {
		this.selectedConstructorSignature = selectedConstructorSignature;
	}

	/**
	 * Builds the specified instance.
	 * 
	 * @param context Contains contextual information to use.
	 * @return The specified instance.
	 * @throws Exception If the creation or configuration of the specified instance
	 *                   fails.
	 */
	public Object build(InstantiationContext context) throws Exception {
		Solution solutionInstance = context.getSolutionInstance();
		InstanceBuilderFacade instanceBuilderFacade = (InstanceBuilderFacade) Facade.get(this,
				context.getParentFacade(), solutionInstance);
		ITypeInfo typeInfo = instanceBuilderFacade.getTypeInfo();
		IMethodInfo constructor = InstantiationUtils.getConstructorInfo(typeInfo, selectedConstructorSignature);
		if (constructor == null) {
			String actualTypeName = computeActualTypeName(
					InstantiationUtils.getAncestorInstanceBuilders(context.getParentFacade()), solutionInstance);
			if (selectedConstructorSignature == null) {
				throw new PotentialError("Cannot create '" + actualTypeName + "' instance: No constructor available");
			} else {
				throw new PotentialError("Cannot create '" + actualTypeName + "' instance: Constructor not found: '"
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
							TypeInfoProvider.getTypeInfo(Object.class.getName(), solutionInstance),
							new InstantiationContext(context, listItemInitializerFacade));
					if (iterationListValue == null) {
						throw new UnexpectedError("Cannot replicate item: Iteration list value is null");
					}
					ITypeInfo actualIterationListType = TypeInfoProvider
							.getTypeInfo(iterationListValue.getClass().getName(), solutionInstance);
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
										: TypeInfoProvider.getTypeInfo(Object.class.getName(), solutionInstance),
								new InstantiationContext(new InstantiationContext(context, listItemInitializerFacade),
										iterationVariable));
						itemList.add(itemValue);
					}
				} else {
					Object itemValue = InstantiationUtils.interpretValue(
							listItemInitializerFacade.getUnderlying().getItemValue(),
							(listTypeInfo.getItemType() != null) ? listTypeInfo.getItemType()
									: TypeInfoProvider.getTypeInfo(Object.class.getName(), solutionInstance),
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
