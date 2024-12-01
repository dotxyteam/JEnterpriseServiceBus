package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ReflectionUIUtils;

public class ObjectSpecification {

	private String objectClassName;
	private List<FieldInitializer> fieldInitializers = new ArrayList<FieldInitializer>();

	public ObjectSpecification() {
	}

	public ObjectSpecification(String objectClassName) {
		this.objectClassName = objectClassName;
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

	public Object build(Plan.ExecutionContext context) throws Exception {
		Class<?> objectClass = Class.forName(objectClassName);
		Object object = objectClass.getConstructor().newInstance();
		ReflectionUI reflectionUI = ReflectionUI.getDefault();
		ITypeInfo typeInfo = reflectionUI.buildTypeInfo(new JavaTypeInfoSource(reflectionUI, objectClass, null));
		for (IFieldInfo field : typeInfo.getFields()) {
			FieldInitializer fieldInitialiser = getFieldInitializer(field.getName());
			if (fieldInitialiser == null) {
				continue;
			}
			Object fieldValue;
			if (fieldInitialiser.getFieldValue() instanceof DynamicValue) {
				fieldValue = Utils.executeScript(((DynamicValue) fieldInitialiser.getFieldValue()).getScript(),
						context);
			} else if (fieldInitialiser.getFieldValue() instanceof ObjectSpecification) {
				fieldValue = ((ObjectSpecification) fieldInitialiser.getFieldValue()).build(context);
			} else {
				fieldValue = fieldInitialiser.getFieldValue();
			}
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

		public Object getFieldValue() {
			return fieldValue;
		}

		public void setFieldValue(Object fieldValue) {
			this.fieldValue = fieldValue;
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

	}

	public static class ObjectSpecificationFacade implements FacadeNode {

		private FieldInitializerFacade parent;
		private ObjectSpecification objectSpecification;

		public ObjectSpecificationFacade(FieldInitializerFacade parent, ObjectSpecification objectSpecification) {
			this.parent = parent;
			this.objectSpecification = objectSpecification;
		}

		public FieldInitializerFacade getParent() {
			return parent;
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
				result.add(new FieldInitializerFacade(this, field.getName()));
			}
			Collections.sort(result, new Comparator<FacadeNode>() {
				@Override
				public int compare(FacadeNode o1, FacadeNode o2) {
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
				}
			});
			return result;
		}

		@Override
		public String toString() {
			return objectSpecification.getObjectClassName();
		}

	}

	public static class FieldInitializerFacade implements FacadeNode {

		public enum ValueMode {
			STATIC_VALUE, DYNAMIC_VALUE, OBJECT_SPECIFICATION
		}

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

		public boolean isConcreteNode() {
			if (parent.getParent() != null) {
				if (!parent.getParent().isConcreteNode()) {
					return false;
				}
			}
			return getFieldInitializer() != null;
		}

		public void setConcreteNode(boolean b) {
			if (b == isConcreteNode()) {
				return;
			}
			if (b) {
				if (parent.getParent() != null) {
					if (!parent.getParent().isConcreteNode()) {
						parent.getParent().setConcreteNode(true);
					}
				}
				parent.getObjectSpecification().getFieldInitializers().add(new FieldInitializer(fieldName, fieldValue));
			} else {
				parent.getObjectSpecification().removeFieldInitializer(fieldName);
				fieldValue = createDefaultFieldValue();
			}
		}

		public ValueMode getValueMode() {
			FieldInitializer fieldInitializer = getFieldInitializer();
			if (fieldInitializer == null) {
				return null;
			}
			if (fieldInitializer.getFieldValue() instanceof DynamicValue) {
				return ValueMode.DYNAMIC_VALUE;
			} else if (fieldInitializer.getFieldValue() instanceof ObjectSpecification) {
				return ValueMode.OBJECT_SPECIFICATION;
			} else {
				return ValueMode.STATIC_VALUE;
			}
		}

		public void setValueMode(ValueMode valueMode) {
			FieldInitializer fieldInitializer = getFieldInitializer();
			if (fieldInitializer == null) {
				return;
			}
			if (valueMode == getValueMode()) {
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
			return fieldName;
		}

	}

}
