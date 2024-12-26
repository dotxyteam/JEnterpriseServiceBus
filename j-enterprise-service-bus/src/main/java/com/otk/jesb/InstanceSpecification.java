package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.otk.jesb.Plan.ExecutionContext.Property;
import com.otk.jesb.meta.ClassProvider;
import com.otk.jesb.meta.TypeInfoProvider;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.method.InvocationData;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.map.IMapEntryTypeInfo;
import xy.reflect.ui.info.type.iterable.map.MapEntryTypeInfoProxy;
import xy.reflect.ui.info.type.iterable.map.StandardMapEntry;
import xy.reflect.ui.info.type.iterable.map.StandardMapEntryTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ReflectionUIUtils;

public class InstanceSpecification {

	private String typeName;
	private List<ParameterInitializer> parameterInitializers = new ArrayList<ParameterInitializer>();
	private List<FieldInitializer> fieldInitializers = new ArrayList<FieldInitializer>();
	private List<ListItemInitializer> listItemInitializers = new ArrayList<ListItemInitializer>();
	private String selectedConstructorSignature;

	public InstanceSpecification() {
	}

	public InstanceSpecification(String typeName) {
		this.typeName = typeName;
	}

	public InstanceSpecificationFacade getFacade() {
		return new InstanceSpecificationFacade(null, this);
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
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

	public Object build(Plan.ExecutionContext context) throws Exception {
		ITypeInfo typeInfo = getTypeInfo();
		IMethodInfo constructor = MiscUtils.getConstructorInfo(typeInfo, selectedConstructorSignature);
		if (constructor == null) {
			if (selectedConstructorSignature == null) {
				throw new AssertionError("Cannot create '" + typeName + "' instance: No constructor available");
			} else {
				throw new AssertionError("Cannot create '" + typeName + "' instance: Constructor not found: '"
						+ selectedConstructorSignature + "'");
			}
		}
		Object[] parameterValues = new Object[constructor.getParameters().size()];
		for (IParameterInfo parameterInfo : constructor.getParameters()) {
			ParameterInitializer parameterInitializer = getParameterInitializer(parameterInfo.getPosition(),
					parameterInfo.getType().getName());
			Object parameterValue;
			if (parameterInitializer == null) {
				parameterValue = MiscUtils
						.interpretValue(MiscUtils.getDefaultInterpretableValue(parameterInfo.getType()), context);
			} else {
				parameterValue = MiscUtils.interpretValue(parameterInitializer.getParameterValue(), context);
			}
			parameterValues[parameterInfo.getPosition()] = parameterValue;
		}
		Object object;
		if (typeInfo instanceof IListTypeInfo) {
			IListTypeInfo listTypeInfo = (IListTypeInfo) typeInfo;
			List<Object> itemList = new ArrayList<Object>();
			for (ListItemInitializer listItemInitializer : listItemInitializers) {
				if (!MiscUtils.isConditionFullfilled(listItemInitializer.getCondition(), context)) {
					continue;
				}
				ListItemReplication itemReplication = listItemInitializer.getItemReplication();
				if (itemReplication != null) {
					Object iterationListValue = MiscUtils.interpretValue(itemReplication.getIterationListValue(),
							context);
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
						Plan.ExecutionContext iterationContext = new Plan.ExecutionContext(context,
								new ListItemReplication.IterationVariable(itemReplication, iterationVariableValue));
						Object itemValue = MiscUtils.interpretValue(listItemInitializer.getItemValue(),
								iterationContext);
						itemList.add(itemValue);
					}
				} else {
					Object itemValue = MiscUtils.interpretValue(listItemInitializer.getItemValue(), context);
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
			Object fieldValue = MiscUtils.interpretValue(fieldInitializer.getFieldValue(), context);
			field.setValue(object, fieldValue);
		}
		return object;
	}

	public ITypeInfo getTypeInfo() {
		return TypeInfoProvider.getTypeInfo(typeName);
	}

	public ParameterInitializer getParameterInitializer(int parameterPosition, String parameterTypeName) {
		for (ParameterInitializer parameterInitializer : parameterInitializers) {
			if ((parameterInitializer.getParameterPosition() == parameterPosition)
					&& (parameterInitializer.getParameterTypeName() == parameterTypeName)) {
				return parameterInitializer;
			}
		}
		return null;
	}

	public void removeParameterInitializer(int parameterPosition, String parameterTypeName) {
		for (Iterator<ParameterInitializer> it = parameterInitializers.iterator(); it.hasNext();) {
			ParameterInitializer parameterInitializer = it.next();
			if ((parameterInitializer.getParameterPosition() == parameterPosition)
					&& (parameterInitializer.getParameterTypeName() == parameterTypeName)) {
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

	public static class MapEntrySpecification extends InstanceSpecification {
		private String keyTypeName;
		private String valueTypeName;

		public MapEntrySpecification(String keyTypeName, String valueTypeName) {
			super(StandardMapEntry.class.getName());
			this.keyTypeName = keyTypeName;
			this.valueTypeName = valueTypeName;
		}

		@Override
		public ITypeInfo getTypeInfo() {
			ReflectionUI reflectionUI = ReflectionUI.getDefault();
			Class<?> keyClass = (keyTypeName != null) ? ClassProvider.getClass(keyTypeName) : null;
			Class<?> valueClass = (valueTypeName != null) ? ClassProvider.getClass(valueTypeName) : null;
			return reflectionUI.getTypeInfo(new JavaTypeInfoSource(reflectionUI, StandardMapEntry.class,
					new Class[] { keyClass, valueClass }, null));
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
		private DynamicValue condition;

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

		public DynamicValue getCondition() {
			return condition;
		}

		public void setCondition(DynamicValue condition) {
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
		private DynamicValue condition;

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

		public DynamicValue getCondition() {
			return condition;
		}

		public void setCondition(DynamicValue condition) {
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

		public static class IterationVariable implements Property {

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

	public static class DynamicValue {
		private String script;

		public DynamicValue() {
		}

		public DynamicValue(String content) {
			this.script = content;
		}

		public String getScript() {
			return script;
		}

		public void setScript(String script) {
			this.script = script;
		}

	}

	public static interface FacadeNode {

		List<FacadeNode> getChildren();

		boolean isConcrete();

		void setConcrete(boolean b);

	}

	public static class InstanceSpecificationFacade implements FacadeNode {

		private FacadeNode parent;
		private InstanceSpecification underlying;

		public InstanceSpecificationFacade(FacadeNode parent, InstanceSpecification instanceSpecification) {
			this.parent = parent;
			this.underlying = instanceSpecification;
		}

		public FacadeNode getParent() {
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

		public InstanceSpecification getUnderlying() {
			return underlying;
		}

		public ITypeInfo getTypeInfo() {
			ITypeInfo result = TypeInfoProvider.getTypeInfo(underlying.getTypeName());
			if (result instanceof IListTypeInfo) {
				if (parent instanceof FieldInitializerFacade) {
					FieldInitializerFacade listFieldInitializerFacade = (FieldInitializerFacade) parent;
					IFieldInfo listFieldInfo = listFieldInitializerFacade.getFieldInfo();
					result = TypeInfoProvider.getTypeInfo(underlying.getTypeName(), listFieldInfo);
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
		public List<FacadeNode> getChildren() {
			List<FacadeNode> result = new ArrayList<InstanceSpecification.FacadeNode>();
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
			Collections.sort(result, new Comparator<FacadeNode>() {
				List<Class<?>> CLASSES_ORDER = Arrays.asList(ParameterInitializerFacade.class,
						FieldInitializerFacade.class, ListItemInitializerFacade.class);

				@Override
				public int compare(FacadeNode o1, FacadeNode o2) {
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
			return "<" + underlying.getTypeName() + ">";
		}

	}

	public static class MapEntrySpecificationFacade extends InstanceSpecificationFacade {

		public MapEntrySpecificationFacade(FacadeNode parent, MapEntrySpecification mapEntrySpecification) {
			super(parent, mapEntrySpecification);
		}

		@Override
		public void setTypeName(String typeName) {
			throw new UnsupportedOperationException("Cannot change map entry type name");
		}

		@Override
		public MapEntrySpecification getUnderlying() {
			return (MapEntrySpecification) super.getUnderlying();
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
		STATIC_VALUE, DYNAMIC_VALUE, OBJECT_SPECIFICATION
	}

	public static class ParameterInitializerFacade implements FacadeNode {

		private InstanceSpecificationFacade parent;
		private int parameterPosition;

		public ParameterInitializerFacade(InstanceSpecificationFacade parent, int parameterPosition) {
			this.parent = parent;
			this.parameterPosition = parameterPosition;
		}

		public InstanceSpecificationFacade getParent() {
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

		public ParameterInitializer getUnderlying() {
			IParameterInfo parameter = getParameterInfo();
			ParameterInitializer result = parent.getUnderlying().getParameterInitializer(parameterPosition,
					parameter.getType().getName());
			if (result == null) {
				result = new ParameterInitializer(parameterPosition, parameter.getType().getName(),
						MiscUtils.getDefaultInterpretableValue(parameter.getType()));
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
			return getParameterInfo().getType().getName();
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
			if (valueMode == ValueMode.DYNAMIC_VALUE) {
				String scriptContent;
				if (!MiscUtils.isComplexType(parameter.getType())) {
					Object defaultValue = ReflectionUIUtils.createDefaultInstance(parameter.getType());
					scriptContent = "return " + ((defaultValue instanceof String) ? ("\"" + defaultValue + "\"")
							: String.valueOf(defaultValue)) + ";";
				} else {
					scriptContent = "return null;";
				}
				setParameterValue(new DynamicValue(scriptContent));
			} else if (valueMode == ValueMode.OBJECT_SPECIFICATION) {
				setParameterValue(new InstanceSpecification(parameter.getType().getName()));
			} else {
				if (!MiscUtils.isComplexType(parameter.getType())) {
					setParameterValue(ReflectionUIUtils.createDefaultInstance(parameter.getType()));
				} else {
					setParameterValue(null);
				}
			}
		}

		public Object getParameterValue() {
			ParameterInitializer parameterInitializer = getUnderlying();
			return parameterInitializer.getParameterValue();
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
		public List<FacadeNode> getChildren() {
			List<FacadeNode> result = new ArrayList<InstanceSpecification.FacadeNode>();
			Object parameterValue = getParameterValue();
			if (parameterValue instanceof InstanceSpecification) {
				result.add(new InstanceSpecificationFacade(this, (InstanceSpecification) parameterValue));
			}
			return result;
		}

		@Override
		public String toString() {
			return "(" + getParameterInfo().getName() + ")";
		}

	}

	public static class FieldInitializerFacade implements FacadeNode {

		private InstanceSpecificationFacade parent;
		private String fieldName;
		private Object fieldValue;

		public FieldInitializerFacade(InstanceSpecificationFacade parent, String fieldName) {
			this.parent = parent;
			this.fieldName = fieldName;
			FieldInitializer fieldInitializer = getUnderlying();
			if (fieldInitializer == null) {
				this.fieldValue = createDefaultFieldValue();
			} else {
				this.fieldValue = fieldInitializer.getFieldValue();
			}
		}

		public InstanceSpecificationFacade getParent() {
			return parent;
		}

		private Object createDefaultFieldValue() {
			IFieldInfo field = getFieldInfo();
			return MiscUtils.getDefaultInterpretableValue(field.getType());
		}

		public IFieldInfo getFieldInfo() {
			ITypeInfo parentTypeInfo = parent.getTypeInfo();
			return ReflectionUIUtils.findInfoByName(parentTypeInfo.getFields(), fieldName);
		}

		public FieldInitializer getUnderlying() {
			return parent.getUnderlying().getFieldInitializer(fieldName);
		}

		public String getFieldName() {
			return fieldName;
		}

		public String getFieldTypeName() {
			return getFieldInfo().getType().getName();
		}

		public DynamicValue getCondition() {
			FieldInitializer fieldInitializer = getUnderlying();
			if (fieldInitializer == null) {
				return null;
			}
			return fieldInitializer.getCondition();
		}

		public void setCondition(DynamicValue condition) {
			setConcrete(true);
			FieldInitializer fieldInitializer = getUnderlying();
			if ((condition != null) && (condition.getScript() == null)) {
				condition = new DynamicValue("return true;");
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
			Object newFieldValue;
			if (valueMode == ValueMode.DYNAMIC_VALUE) {
				String scriptContent;
				if (!MiscUtils.isComplexType(field.getType())) {
					Object defaultValue = ReflectionUIUtils.createDefaultInstance(field.getType());
					scriptContent = "return " + ((defaultValue instanceof String) ? ("\"" + defaultValue + "\"")
							: String.valueOf(defaultValue)) + ";";
				} else {
					scriptContent = "return null;";
				}
				newFieldValue = new DynamicValue(scriptContent);
			} else if (valueMode == ValueMode.OBJECT_SPECIFICATION) {
				newFieldValue = new InstanceSpecification(field.getType().getName());
			} else if (valueMode == ValueMode.STATIC_VALUE) {
				if (!MiscUtils.isComplexType(field.getType())) {
					newFieldValue = ReflectionUIUtils.createDefaultInstance(field.getType());
				} else {
					newFieldValue = null;
				}
			} else {
				newFieldValue = null;
			}
			setFieldValue(newFieldValue);
		}

		public Object getFieldValue() {
			FieldInitializer fieldInitializer = getUnderlying();
			if (fieldInitializer == null) {
				return null;
			}
			return fieldInitializer.getFieldValue();
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
		public List<FacadeNode> getChildren() {
			List<FacadeNode> result = new ArrayList<InstanceSpecification.FacadeNode>();
			if (fieldValue instanceof InstanceSpecification) {
				result.add(new InstanceSpecificationFacade(this, (InstanceSpecification) fieldValue));
			}
			return result;
		}

		@Override
		public String toString() {
			return fieldName + ((getCondition() != null) ? "?" : "");
		}

	}

	public static class ListItemInitializerFacade implements FacadeNode {

		private InstanceSpecificationFacade parent;
		private int index;
		private Object itemValue;

		public ListItemInitializerFacade(InstanceSpecificationFacade parent, int index) {
			this.parent = parent;
			this.index = index;
			ListItemInitializer listItemInitializer = getUnderlying();
			if (listItemInitializer == null) {
				this.itemValue = createDefaultItemValue();
			} else {
				this.itemValue = listItemInitializer.getItemValue();
			}
		}

		public InstanceSpecificationFacade getParent() {
			return parent;
		}

		public int getIndex() {
			return index;
		}

		public String getItemTypeName() {
			ITypeInfo itemType = getItemType();
			if (itemType == null) {
				return null;
			}
			return itemType.getName();
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

		public DynamicValue getCondition() {
			ListItemInitializer listItemInitializer = getUnderlying();
			if (listItemInitializer == null) {
				return null;
			}
			return listItemInitializer.getCondition();
		}

		public void setCondition(DynamicValue condition) {
			setConcrete(true);
			ListItemInitializer listItemInitializer = getUnderlying();
			if ((condition != null) && (condition.getScript() == null)) {
				condition = new DynamicValue("return true;");
			}
			listItemInitializer.setCondition(condition);
		}

		public Object getItemValue() {
			ListItemInitializer listItemInitializer = getUnderlying();
			if (listItemInitializer == null) {
				return null;
			}
			return listItemInitializer.getItemValue();
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
			if (valueMode == ValueMode.DYNAMIC_VALUE) {
				String scriptContent;
				if (!MiscUtils.isComplexType(itemType)) {
					Object defaultValue = ReflectionUIUtils.createDefaultInstance(itemType);
					scriptContent = "return " + ((defaultValue instanceof String) ? ("\"" + defaultValue + "\"")
							: String.valueOf(defaultValue)) + ";";
				} else {
					scriptContent = "return null;";
				}
				itemValue = new DynamicValue(scriptContent);
			} else if (valueMode == ValueMode.OBJECT_SPECIFICATION) {
				itemValue = new InstanceSpecification(itemType.getName());
			} else {
				if (!MiscUtils.isComplexType(itemType)) {
					itemValue = ReflectionUIUtils.createDefaultInstance(itemType);
				} else {
					itemValue = null;
				}
			}
			listItemInitializer.setItemValue(itemValue);
		}

		private Object createDefaultItemValue() {
			ITypeInfo itemType = getItemType();
			return MiscUtils.getDefaultInterpretableValue(itemType);
		}

		public ITypeInfo getItemType() {
			ITypeInfo parentTypeInfo = parent.getTypeInfo();
			return ((IListTypeInfo) parentTypeInfo).getItemType();
		}

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
		public List<FacadeNode> getChildren() {
			List<FacadeNode> result = new ArrayList<InstanceSpecification.FacadeNode>();
			if (itemValue instanceof MapEntrySpecification) {
				result.add(new MapEntrySpecificationFacade(this, (MapEntrySpecification) itemValue));
			} else if (itemValue instanceof InstanceSpecification) {
				result.add(new InstanceSpecificationFacade(this, (InstanceSpecification) itemValue));
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
			return listItemReplication.getIterationListValue();
		}

		public void setIterationListValue(Object iterationListValue) {
			listItemReplication.setIterationListValue(iterationListValue);
		}

		public ValueMode getIterationListValueMode() {
			return MiscUtils.getValueMode(listItemReplication.getIterationListValue());
		}

		public void setIterationListValueMode(ValueMode valueMode) {
			Object iterationListValue;
			if (valueMode == ValueMode.DYNAMIC_VALUE) {
				String scriptContent = "return new java.util.ArrayList<Object>();";
				iterationListValue = new DynamicValue(scriptContent);
			} else if (valueMode == ValueMode.OBJECT_SPECIFICATION) {
				iterationListValue = new InstanceSpecification(ArrayList.class.getName());
			} else {
				iterationListValue = new ArrayList<Object>();
			}
			listItemReplication.setIterationListValue(iterationListValue);
		}
	}

}
