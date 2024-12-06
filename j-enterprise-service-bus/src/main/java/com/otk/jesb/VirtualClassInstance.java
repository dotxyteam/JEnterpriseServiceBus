package com.otk.jesb;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import xy.reflect.ui.info.field.FieldInfoProxy;
import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.type.BasicTypeInfoProxy;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.EnumerationTypeInfoProxy;
import xy.reflect.ui.info.type.enumeration.IEnumerationItemInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;
import xy.reflect.ui.info.type.iterable.ListTypeInfoProxy;
import xy.reflect.ui.util.MiscUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public abstract class VirtualClassInstance {

	protected abstract VirtualClassDescription getVirtualClassDescription();

	private static String getVirtualClassName(VirtualClassDescription virtualClassDescription) {
		return virtualClassDescription.getName().replaceAll("[^a-zA-Z0-9]", "_");
	}

	public static interface VirtualClassDescription {
		public String generateInstanciationScript(String virtualClassInstanceVariableName);

		public String getName();

	}

	public static abstract class VirtualBasicClassDescription extends BasicTypeInfoProxy
			implements VirtualClassDescription {

		public VirtualBasicClassDescription() {
			super(ITypeInfo.NULL_BASIC_TYPE_INFO);
		}

		@Override
		public abstract String getName();

		protected abstract List<VirtualFieldDescription> getVirtualFieldDescriptions();

		@Override
		public final List<IMethodInfo> getConstructors() {
			throw new AssertionError();
		}

		@Override
		public final List<IFieldInfo> getFields() {
			return new ArrayList<IFieldInfo>(getVirtualFieldDescriptions());
		}

		@Override
		public final List<IMethodInfo> getMethods() {
			throw new AssertionError();
		}

		@Override
		public String generateInstanciationScript(String virtualClassInstanceVariableName) {
			StringBuilder result = new StringBuilder();
			for (VirtualFieldDescription virtualFieldDescription : getVirtualFieldDescriptions()) {
				if (virtualFieldDescription.getType() instanceof VirtualClassDescription) {
					result.append(((VirtualClassDescription) virtualFieldDescription.getType())
							.generateInstanciationScript(null) + "\n");
				}
			}
			result.append("public class " + getVirtualClassName(this) + "{" + "\n");
			result.append("  private " + VirtualClassInstance.class.getName() + " source;" + "\n");
			result.append("  private " + ITypeInfo.class.getName() + " typeInfo;" + "\n");
			result.append("  public " + getVirtualClassName(this) + "(" + VirtualClassInstance.class.getName()
					+ " source) {" + "\n");
			result.append("    this.source = source;" + "\n");
			result.append("    this.typeInfo = source.getVirtualClassDescription();" + "\n");
			result.append("  }" + "\n");
			for (IFieldInfo fieldInfo : getFields()) {
				String getterMethoName = fieldInfo.getName().substring(0, 1).toUpperCase()
						+ fieldInfo.getName().substring(1);
				result.append("  public " + fieldInfo.getType().getName() + " get" + getterMethoName + "() {" + "\n");
				result.append("    " + IFieldInfo.class.getName() + " fieldInfo = " + ReflectionUIUtils.class.getName()
						+ ".findInfoByName(typeInfo.getFields(), \"" + fieldInfo.getName() + "\");" + "\n");
				result.append("    return (" + fieldInfo.getType().getName() + ") fieldInfo.getValue(source);" + "\n");
				result.append("  }" + "\n");
			}
			result.append("}" + "\n");
			if (virtualClassInstanceVariableName != null) {
				result.append("return new " + getVirtualClassName(this) + "(" + virtualClassInstanceVariableName + ");"
						+ "\n");
			}
			return result.toString();
		}

	}

	public static abstract class VirtualFieldDescription extends FieldInfoProxy {

		public VirtualFieldDescription() {
			super(IFieldInfo.NULL_FIELD_INFO);
		}

		@Override
		public abstract String getName();

		@Override
		public abstract String getCaption();

		@Override
		public abstract Object getValue(Object object);

		@Override
		public abstract ITypeInfo getType();

		@Override
		public final Object[] getValueOptions(Object object) {
			throw new AssertionError();
		}

		@Override
		public final void setValue(Object object, Object value) {
			throw new AssertionError();
		}

	}

	public static abstract class VirtualListClassDescription extends ListTypeInfoProxy
			implements VirtualClassDescription {

		public VirtualListClassDescription() {
			super(IListTypeInfo.NULL_LIST_TYPE_INFO);
		}

		@Override
		public abstract ITypeInfo getItemType();

		@Override
		public abstract Object[] toArray(Object listValue);

		@Override
		public final Object fromArray(Object[] array) {
			throw new AssertionError();
		}

		@Override
		public final void replaceContent(Object listValue, Object[] array) {
			throw new AssertionError();
		}

		@Override
		public String generateInstanciationScript(String virtualClassInstanceVariableName) {
			StringBuilder result = new StringBuilder();
			String itemTypeName = (getItemType() == null) ? Object.class.getName() : getItemType().getName();
			result.append("public class " + getVirtualClassName(this) + " extends " + AbstractList.class.getName() + "<"
					+ itemTypeName + ">{" + "\n");
			result.append("  private Object[] sourceArray;" + "\n");
			result.append("  public test(" + VirtualClassInstance.class.getName() + " source) {" + "\n");
			result.append("    " + IListTypeInfo.class.getName() + " listTypeInfo = (" + IListTypeInfo.class.getName()
					+ ") source.getTypeInfo();" + "\n");
			result.append("    this.sourceArray = listTypeInfo.toArray(source);" + "\n");
			result.append("  }" + "\n");
			result.append("  @Override" + "\n");
			result.append("  public " + itemTypeName + " get(int index) {" + "\n");
			result.append("    return (" + itemTypeName + ")sourceArray[index];" + "\n");
			result.append("  }" + "\n");
			result.append("  @Override" + "\n");
			result.append("  public int size() {" + "\n");
			result.append("    return sourceArray.length;" + "\n");
			result.append("  }" + "\n");
			result.append("}" + "\n");
			result.append(
					"return new " + getVirtualClassName(this) + "(" + virtualClassInstanceVariableName + ");" + "\n");
			return result.toString();
		}

	}

	public static abstract class VirtualEnumerationClassDescription extends EnumerationTypeInfoProxy
			implements VirtualClassDescription {

		public VirtualEnumerationClassDescription() {
			super(IEnumerationTypeInfo.NULL_ENUMERATION_TYPE_INFO);
		}

		@Override
		public abstract Object[] getValues();

		@Override
		public abstract IEnumerationItemInfo getValueInfo(Object value);

		@Override
		public String generateInstanciationScript(String virtualClassInstanceVariableName) {
			StringBuilder result = new StringBuilder();
			result.append("public enum " + getVirtualClassName(this) + "{" + "\n");
			List<String> enumItemNames = new ArrayList<String>();
			for (Object enumItem : getValues()) {
				IEnumerationItemInfo enumItemInfo = getValueInfo(enumItem);
				enumItemNames.add(enumItemInfo.getName());
			}
			result.append("  " + MiscUtils.stringJoin(enumItemNames, ", ") + ";\n");
			result.append("  public static " + getVirtualClassName(this) + " get("
					+ VirtualClassInstance.class.getName() + " source) {" + "\n");
			result.append("    " + IEnumerationTypeInfo.class.getName() + " enumTypeInfo = ("
					+ IEnumerationTypeInfo.class.getName() + ") source.getVirtualClassDescription();" + "\n");
			result.append("    " + IEnumerationItemInfo.class.getName()
					+ " enuimItemInfo = enumTypeInfo.getValueInfo(source);" + "\n");
			for (Object enumItem : getValues()) {
				IEnumerationItemInfo enumItemInfo = getValueInfo(enumItem);
				result.append("    if (enuimItemInfo.getName().equals(\"" + enumItemInfo.getName() + "\")) {" + "\n");
				result.append("      return " + enumItemInfo.getName() + ";" + "\n");
				result.append("    } else " + "\n");
			}
			result.append("    {" + "\n");
			result.append("      throw new " + AssertionError.class.getName() + "();" + "\n");
			result.append("    }" + "\n");
			result.append("  }" + "\n");
			result.append("}" + "\n");
			result.append(
					"return new " + getVirtualClassName(this) + "(" + virtualClassInstanceVariableName + ");" + "\n");
			return result.toString();
		}
	}

}
