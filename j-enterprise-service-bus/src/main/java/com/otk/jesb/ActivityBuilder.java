package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

import xy.reflect.ui.ReflectionUI;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;

public class ActivityBuilder {
	private String activityClassName;
	private List<FieldInitialiser> fieldInitialisers = new ArrayList<FieldInitialiser>();

	public String getActivityClassName() {
		return activityClassName;
	}

	public void setActivityClassName(String activityClassName) {
		this.activityClassName = activityClassName;
	}

	public List<FieldInitialiser> getFieldInitialisers() {
		return fieldInitialisers;
	}

	public void setFieldInitialisers(List<FieldInitialiser> fieldInitialisers) {
		this.fieldInitialisers = fieldInitialisers;
	}

	public Activity build(Plan.ExecutionContext context) throws Exception {
		Class<?> activityClass = Class.forName(getActivityClassName());
		Activity activity = (Activity) activityClass.getConstructor().newInstance();
		ReflectionUI reflectionUI = ReflectionUI.getDefault();
		ITypeInfo typeInfo = reflectionUI.buildTypeInfo(new JavaTypeInfoSource(reflectionUI, activityClass, null));
		for (IFieldInfo field : typeInfo.getFields()) {
			FieldInitialiser fieldInitialiser = getFieldInitialiser(field.getName());
			if (fieldInitialiser == null) {
				continue;
			}
			Object fieldValue;
			if (fieldInitialiser.isFieldValueDynamic()) {
				fieldValue = Utils.executeCode((String) fieldInitialiser.getFieldValue(), context);
			} else {
				fieldValue = fieldInitialiser.getFieldValue();
			}
			field.setValue(activity, fieldValue);
		}
		return activity;
	}

	private FieldInitialiser getFieldInitialiser(String fieldName) {
		for (FieldInitialiser fieldInitialiser : fieldInitialisers) {
			if (fieldInitialiser.getFieldName().equals(fieldName)) {
				return fieldInitialiser;
			}
		}
		return null;
	}

	public static class FieldInitialiser {
		private String fieldName;
		private boolean fieldValueDynamic = false;
		private Object fieldValue;

		public FieldInitialiser() {
		}

		public FieldInitialiser(String fieldName, boolean fieldValueDynamic, Object fieldValue) {
			super();
			this.fieldName = fieldName;
			this.fieldValueDynamic = fieldValueDynamic;
			this.fieldValue = fieldValue;
		}

		public String getFieldName() {
			return fieldName;
		}

		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		public boolean isFieldValueDynamic() {
			return fieldValueDynamic;
		}

		public void setFieldValueDynamic(boolean fieldValueDynamic) {
			this.fieldValueDynamic = fieldValueDynamic;
		}

		public Object getFieldValue() {
			return fieldValue;
		}

		public void setFieldValue(Object fieldValue) {
			this.fieldValue = fieldValue;
		}

	}
}
