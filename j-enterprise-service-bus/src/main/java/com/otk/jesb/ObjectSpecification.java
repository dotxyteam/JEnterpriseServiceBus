package com.otk.jesb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.otk.jesb.Plan.ExecutionContext.Property;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.field.GetterFieldInfo;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.field.PublicFieldInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ReflectionUIUtils;

public class ObjectSpecification {

	private String objectClassName;
	private List<FieldInitializer> fieldInitializers = new ArrayList<FieldInitializer>();
	private List<ListItemInitializer> listItemInitializers = new ArrayList<ListItemInitializer>();

	public ObjectSpecification() {
	}

	public ObjectSpecification(String objectClassName) {
		this.objectClassName = objectClassName;
	}

	public ObjectSpecificationFacade getFacade() {
		return new ObjectSpecificationFacade(null, this);
	}

	public String getObjectClassName() {
		return objectClassName;
	}

	public void setObjectClassName(String objectClassName) {
		this.objectClassName = objectClassName;
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
		Class<?> objectClass = Class.forName(objectClassName);
		ReflectionUI reflectionUI = ReflectionUI.getDefault();
		ITypeInfo typeInfo = reflectionUI.buildTypeInfo(new JavaTypeInfoSource(reflectionUI, objectClass, null));
		Object object;
		if (typeInfo instanceof IListTypeInfo) {
			IListTypeInfo listTypeInfo = (IListTypeInfo) typeInfo;
			List<Object> itemList = new ArrayList<Object>();
			for (ListItemInitializer listItemInitializer : listItemInitializers) {
				if (!Utils.isConditionFullfilled(listItemInitializer.getCondition(), context)) {
					continue;
				}
				ListItemReplication itemReplication = listItemInitializer.getItemReplication();
				if (itemReplication != null) {
					Object iterationListValue = Utils.interpretValue(itemReplication.getIterationListValue(), context);
					if (iterationListValue == null) {
						throw new AssertionError("Cannot replicate item: Iteration list value is null");
					}
					ITypeInfo iterationListTypeInfo = reflectionUI
							.buildTypeInfo(new JavaTypeInfoSource(reflectionUI, iterationListValue.getClass(), null));
					if (!(iterationListTypeInfo instanceof IListTypeInfo)) {
						throw new AssertionError("Cannot replicate item: Iteration list value is not iterable: '"
								+ iterationListValue + "'");
					}
					Object[] iterationListArray = ((IListTypeInfo) iterationListTypeInfo).toArray(iterationListValue);
					for (Object iterationVariableValue : iterationListArray) {
						Plan.ExecutionContext iterationContext = new Plan.ExecutionContext(context,
								new ListItemReplication.IterationVariable(itemReplication, iterationVariableValue));
						Object itemValue = Utils.interpretValue(listItemInitializer.getItemValue(), iterationContext);
						itemList.add(itemValue);
					}
				} else {
					Object itemValue = Utils.interpretValue(listItemInitializer.getItemValue(), context);
					itemList.add(itemValue);
				}
			}
			if (listTypeInfo.canReplaceContent()) {
				object = objectClass.getConstructor().newInstance();
				listTypeInfo.replaceContent(object, itemList.toArray());
			} else if (listTypeInfo.canInstanciateFromArray()) {
				object = listTypeInfo.fromArray(itemList.toArray());
			} else {
				throw new AssertionError("Cannot initialize list of type " + listTypeInfo
						+ ": Cannot replace instance content or instanciate from array");
			}
		} else {
			object = objectClass.getConstructor().newInstance();
		}
		for (IFieldInfo field : typeInfo.getFields()) {
			if (field.isGetOnly()) {
				continue;
			}
			FieldInitializer fieldInitializer = getFieldInitializer(field.getName());
			if (fieldInitializer == null) {
				continue;
			}
			if (!Utils.isConditionFullfilled(fieldInitializer.getCondition(), context)) {
				continue;
			}
			Object fieldValue = Utils.interpretValue(fieldInitializer.getFieldValue(), context);
			field.setValue(object, fieldValue);
		}
		return object;
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

	public static class ObjectSpecificationFacade implements FacadeNode {

		private FacadeNode parent;
		private ObjectSpecification objectSpecification;

		public ObjectSpecificationFacade(FacadeNode parent, ObjectSpecification objectSpecification) {
			this.parent = parent;
			this.objectSpecification = objectSpecification;
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

		public String getObjectClassName() {
			return objectSpecification.getObjectClassName();
		}

		public void setObjectClassName(String objectClassName) {
			objectSpecification.setObjectClassName(objectClassName);
		}

		public ObjectSpecification getObjectSpecification() {
			return objectSpecification;
		}

		@Override
		public List<FacadeNode> getChildren() {
			List<FacadeNode> result = new ArrayList<ObjectSpecification.FacadeNode>();
			Class<?> objectClass;
			try {
				objectClass = Class.forName(objectSpecification.getObjectClassName());
			} catch (ClassNotFoundException e) {
				throw new AssertionError(e);
			}
			ReflectionUI reflectionUI = ReflectionUI.getDefault();
			ITypeInfo typeInfo = reflectionUI.buildTypeInfo(new JavaTypeInfoSource(reflectionUI, objectClass, null));
			for (IFieldInfo field : typeInfo.getFields()) {
				if (field.isGetOnly()) {
					continue;
				}
				result.add(new FieldInitializerFacade(this, field.getName()));
			}
			if (typeInfo instanceof IListTypeInfo) {
				int i = 0;
				for (; i < objectSpecification.getListItemInitializers().size();) {
					result.add(new ListItemInitializerFacade(this, i));
					i++;
				}
				result.add(new ListItemInitializerFacade(this, i));
			}
			Collections.sort(result, new Comparator<FacadeNode>() {
				@Override
				public int compare(FacadeNode o1, FacadeNode o2) {
					if ((o1 instanceof FieldInitializerFacade) && (o2 instanceof ListItemInitializerFacade)) {
						return 1;
					} else if ((o1 instanceof ListItemInitializerFacade) && (o2 instanceof FieldInitializerFacade)) {
						return -1;
					} else if ((o1 instanceof FieldInitializerFacade) && (o2 instanceof FieldInitializerFacade)) {
						FieldInitializerFacade fif1 = (FieldInitializerFacade) o1;
						FieldInitializerFacade fif2 = (FieldInitializerFacade) o2;
						if (Utils.isComplexType(fif1.getFieldInfo().getType())
								&& !Utils.isComplexType(fif2.getFieldInfo().getType())) {
							return 1;
						} else if (!Utils.isComplexType(fif1.getFieldInfo().getType())
								&& Utils.isComplexType(fif2.getFieldInfo().getType())) {
							return -1;
						} else {
							return fif1.getFieldInfo().getName().compareTo(fif2.getFieldInfo().getName());
						}
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
			return "<" + objectSpecification.getObjectClassName() + ">";
		}

	}

	public enum ValueMode {
		STATIC_VALUE, DYNAMIC_VALUE, OBJECT_SPECIFICATION
	}

	public static class FieldInitializerFacade implements FacadeNode {

		private ObjectSpecificationFacade parent;
		private String fieldName;
		private Object fieldValue;

		public FieldInitializerFacade(ObjectSpecificationFacade parent, String fieldName) {
			this.parent = parent;
			this.fieldName = fieldName;
			FieldInitializer fieldInitializer = getFieldInitializer();
			if (fieldInitializer == null) {
				this.fieldValue = createDefaultFieldValue();
			} else {
				this.fieldValue = fieldInitializer.getFieldValue();
			}
		}

		public ObjectSpecificationFacade getParent() {
			return parent;
		}

		private Object createDefaultFieldValue() {
			IFieldInfo field = getFieldInfo();
			if (!Utils.isComplexType(field.getType())) {
				return ReflectionUIUtils.createDefaultInstance(field.getType());
			} else {
				return new ObjectSpecification(field.getType().getName());
			}
		}

		public IFieldInfo getFieldInfo() {
			Class<?> objectClass;
			try {
				objectClass = Class.forName(parent.getObjectSpecification().getObjectClassName());
			} catch (ClassNotFoundException e) {
				throw new AssertionError(e);
			}
			ReflectionUI reflectionUI = ReflectionUI.getDefault();
			ITypeInfo typeInfo = reflectionUI.buildTypeInfo(new JavaTypeInfoSource(reflectionUI, objectClass, null));
			return ReflectionUIUtils.findInfoByName(typeInfo.getFields(), fieldName);
		}

		public FieldInitializer getFieldInitializer() {
			return parent.getObjectSpecification().getFieldInitializer(fieldName);
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public DynamicValue getCondition() {
			FieldInitializer fieldInitializer = getFieldInitializer();
			if (fieldInitializer == null) {
				return null;
			}
			return fieldInitializer.getCondition();
		}

		public void setCondition(DynamicValue condition) {
			FieldInitializer fieldInitializer = getFieldInitializer();
			if (fieldInitializer == null) {
				return;
			}
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
			return getFieldInitializer() != null;
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
				parent.getObjectSpecification().getFieldInitializers().add(new FieldInitializer(fieldName, fieldValue));
			} else {
				parent.getObjectSpecification().removeFieldInitializer(fieldName);
				fieldValue = createDefaultFieldValue();
			}
		}

		public ValueMode getFieldValueMode() {
			FieldInitializer fieldInitializer = getFieldInitializer();
			if (fieldInitializer == null) {
				return null;
			}
			return Utils.getValueMode(fieldInitializer.getFieldValue());
		}

		public void setFieldValueMode(ValueMode valueMode) {
			FieldInitializer fieldInitializer = getFieldInitializer();
			if (fieldInitializer == null) {
				return;
			}
			if (valueMode == getFieldValueMode()) {
				return;
			}
			IFieldInfo field = getFieldInfo();
			if (valueMode == ValueMode.DYNAMIC_VALUE) {
				String scriptContent;
				if (!Utils.isComplexType(field.getType())) {
					Object defaultValue = ReflectionUIUtils.createDefaultInstance(field.getType());
					scriptContent = "return " + ((defaultValue instanceof String) ? ("\"" + defaultValue + "\"")
							: String.valueOf(defaultValue)) + ";";
				} else {
					scriptContent = "return null;";
				}
				fieldValue = new DynamicValue(scriptContent);
			} else if (valueMode == ValueMode.OBJECT_SPECIFICATION) {
				fieldValue = new ObjectSpecification(field.getType().getName());
			} else {
				if (!Utils.isComplexType(field.getType())) {
					fieldValue = ReflectionUIUtils.createDefaultInstance(field.getType());
				} else {
					fieldValue = null;
				}
			}
			fieldInitializer.setFieldValue(fieldValue);
		}

		public Object getFieldValue() {
			FieldInitializer fieldInitializer = getFieldInitializer();
			if (fieldInitializer == null) {
				return null;
			}
			return fieldInitializer.getFieldValue();
		}

		public void setFieldValue(Object value) {
			FieldInitializer fieldInitializer = getFieldInitializer();
			if (fieldInitializer == null) {
				return;
			}
			IFieldInfo field = getFieldInfo();
			if ((value == null) && (field.getType().isPrimitive())) {
				throw new AssertionError("Cannot set null to primitive field");
			}
			fieldInitializer.setFieldValue(value);
		}

		@Override
		public List<FacadeNode> getChildren() {
			List<FacadeNode> result = new ArrayList<ObjectSpecification.FacadeNode>();
			if (fieldValue instanceof ObjectSpecification) {
				result.add(new ObjectSpecificationFacade(this, (ObjectSpecification) fieldValue));
			}
			return result;
		}

		@Override
		public String toString() {
			return fieldName + ((getCondition() != null) ? "?" : "");
		}

	}

	public static class ListItemInitializerFacade implements FacadeNode {

		private ObjectSpecificationFacade parent;
		private int index;
		private Object itemValue;

		public ListItemInitializerFacade(ObjectSpecificationFacade parent, int index) {
			this.parent = parent;
			this.index = index;
			ListItemInitializer listItemInitializer = getListItemInitializer();
			if (listItemInitializer == null) {
				this.itemValue = createDefaultItemValue();
			} else {
				this.itemValue = listItemInitializer.getItemValue();
			}
		}

		public ObjectSpecificationFacade getParent() {
			return parent;
		}

		public int getIndex() {
			return index;
		}

		public ListItemReplicationFacade getItemReplicationFacade() {
			ListItemInitializer listItemInitializer = getListItemInitializer();
			if (listItemInitializer == null) {
				return null;
			}
			return (listItemInitializer.getItemReplication() == null) ? null
					: new ListItemReplicationFacade(listItemInitializer.getItemReplication());
		}

		public void setItemReplicationFacade(ListItemReplicationFacade itemReplicationFacade) {
			ListItemInitializer listItemInitializer = getListItemInitializer();
			if (listItemInitializer == null) {
				return;
			}
			listItemInitializer.setItemReplication(
					(itemReplicationFacade == null) ? null : itemReplicationFacade.getListItemReplication());
		}

		public DynamicValue getCondition() {
			ListItemInitializer listItemInitializer = getListItemInitializer();
			if (listItemInitializer == null) {
				return null;
			}
			return listItemInitializer.getCondition();
		}

		public void setCondition(DynamicValue condition) {
			ListItemInitializer listItemInitializer = getListItemInitializer();
			if (listItemInitializer == null) {
				return;
			}
			if ((condition != null) && (condition.getScript() == null)) {
				condition = new DynamicValue("return true;");
			}
			listItemInitializer.setCondition(condition);
		}

		public Object getItemValue() {
			ListItemInitializer listItemInitializer = getListItemInitializer();
			if (listItemInitializer == null) {
				return null;
			}
			return listItemInitializer.getItemValue();
		}

		public void setItemValue(Object value) {
			ListItemInitializer listItemInitializer = getListItemInitializer();
			if (listItemInitializer == null) {
				return;
			}
			ITypeInfo itemType = getItemType();
			if ((value == null) && (itemType != null) && (itemType.isPrimitive())) {
				throw new AssertionError("Cannot add null item to primitive item list");
			}
			listItemInitializer.setItemValue(value);
		}

		public ValueMode getItemValueMode() {
			ListItemInitializer listItemInitializer = getListItemInitializer();
			if (listItemInitializer == null) {
				return null;
			}
			return Utils.getValueMode(listItemInitializer.getItemValue());
		}

		public void setItemValueMode(ValueMode valueMode) {
			ListItemInitializer listItemInitializer = getListItemInitializer();
			if (listItemInitializer == null) {
				return;
			}
			if (valueMode == getItemValueMode()) {
				return;
			}
			ITypeInfo itemType = getItemType();
			if (valueMode == ValueMode.DYNAMIC_VALUE) {
				String scriptContent;
				if (!Utils.isComplexType(itemType)) {
					Object defaultValue = ReflectionUIUtils.createDefaultInstance(itemType);
					scriptContent = "return " + ((defaultValue instanceof String) ? ("\"" + defaultValue + "\"")
							: String.valueOf(defaultValue)) + ";";
				} else {
					scriptContent = "return null;";
				}
				itemValue = new DynamicValue(scriptContent);
			} else if (valueMode == ValueMode.OBJECT_SPECIFICATION) {
				itemValue = new ObjectSpecification(itemType.getName());
			} else {
				if (!Utils.isComplexType(itemType)) {
					itemValue = ReflectionUIUtils.createDefaultInstance(itemType);
				} else {
					itemValue = null;
				}
			}
			listItemInitializer.setItemValue(itemValue);
		}

		private Object createDefaultItemValue() {
			ITypeInfo itemType = getItemType();
			if (itemType == null) {
				return null;
			} else if (!Utils.isComplexType(itemType)) {
				return ReflectionUIUtils.createDefaultInstance(itemType);
			} else {
				return new ObjectSpecification(itemType.getName());
			}
		}

		public ITypeInfo getItemType() {
			Class<?> objectClass;
			try {
				objectClass = Class.forName(parent.getObjectSpecification().getObjectClassName());
			} catch (ClassNotFoundException e) {
				throw new AssertionError(e);
			}
			ReflectionUI reflectionUI = ReflectionUI.getDefault();
			JavaTypeInfoSource javaTypeInfoSource;
			if (parent.getParent() instanceof FieldInitializerFacade) {
				FieldInitializerFacade listFieldInitializerFacade = (FieldInitializerFacade) parent.getParent();
				IFieldInfo listFieldInfo = listFieldInitializerFacade.getFieldInfo();
				if (listFieldInfo instanceof GetterFieldInfo) {
					Method listGetterMethod = ((GetterFieldInfo) listFieldInfo).getJavaGetterMethod();
					javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, listGetterMethod, -1, null);
				} else if (listFieldInfo instanceof PublicFieldInfo) {
					Field listField = ((PublicFieldInfo) listFieldInfo).getJavaField();
					javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, listField, -1, null);
				} else {
					throw new AssertionError();
				}
			} else {
				javaTypeInfoSource = new JavaTypeInfoSource(reflectionUI, objectClass, null);
			}
			ITypeInfo typeInfo = reflectionUI.buildTypeInfo(javaTypeInfoSource);
			return ((IListTypeInfo) typeInfo).getItemType();
		}

		public ListItemInitializer getListItemInitializer() {
			if (index >= parent.getObjectSpecification().getListItemInitializers().size()) {
				return null;
			}
			return parent.getObjectSpecification().getListItemInitializers().get(index);
		}

		@Override
		public boolean isConcrete() {
			if (!parent.isConcrete()) {
				return false;
			}
			return getListItemInitializer() != null;
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
				parent.getObjectSpecification().getListItemInitializers().add(index,
						new ListItemInitializer(itemValue));
			} else {
				parent.getObjectSpecification().getListItemInitializers().remove(index);
				itemValue = createDefaultItemValue();
			}
		}

		@Override
		public List<FacadeNode> getChildren() {
			List<FacadeNode> result = new ArrayList<ObjectSpecification.FacadeNode>();
			if (itemValue instanceof ObjectSpecification) {
				result.add(new ObjectSpecificationFacade(this, (ObjectSpecification) itemValue));
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

		public ListItemReplication getListItemReplication() {
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
			return Utils.getValueMode(listItemReplication.getIterationListValue());
		}

		public void setIterationListValueMode(ValueMode valueMode) {
			Object iterationListValue;
			if (valueMode == ValueMode.DYNAMIC_VALUE) {
				String scriptContent = "return new java.util.ArrayList<Object>();";
				iterationListValue = new DynamicValue(scriptContent);
			} else if (valueMode == ValueMode.OBJECT_SPECIFICATION) {
				iterationListValue = new ObjectSpecification(ArrayList.class.getName());
			} else {
				iterationListValue = new ArrayList<Object>();
			}
			listItemReplication.setIterationListValue(iterationListValue);
		}
	}

}
