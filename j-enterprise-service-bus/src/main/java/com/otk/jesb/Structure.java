package com.otk.jesb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.otk.jesb.util.MiscUtils;

public abstract class Structure {

	public abstract String generateJavaTypeSourceCode(String className);

	public static class ClassicStructure extends Structure {

		private List<Element> elements = new ArrayList<Element>();

		public List<Element> getElements() {
			return elements;
		}

		public void setElements(List<Element> elements) {
			this.elements = elements;
			if (elements != null) {
				Collections.sort(elements, new Comparator<Element>() {
					@Override
					public int compare(Element e1, Element e2) {
						if ((e1.getOptionality() != null) && (e2.getOptionality() == null)) {
							return 1;
						}
						if ((e1.getOptionality() == null) && (e2.getOptionality() != null)) {
							return -1;
						}
						return 0;
					}
				});
			}
		}

		@Override
		public String generateJavaTypeSourceCode(String className) {
			StringBuilder result = new StringBuilder();
			result.append("public class " + className + " implements "
					+ MiscUtils.adaptClassNameToSourceCode(Structured.class.getName()) + "{" + "\n");
			result.append(MiscUtils.stringJoin(elements.stream().map((e) -> e.generateJavaFieldDeclarationSourceCode())
					.collect(Collectors.toList()), "\n") + "\n");
			result.append(
					"public " + className + "("
							+ MiscUtils.stringJoin(elements.stream()
									.map((e) -> e.generateJavaConstructorParameterDeclarationSourceCode())
									.filter(Objects::nonNull).collect(Collectors.toList()), ", ")
							+ "){" + "\n");
			result.append(MiscUtils.stringJoin(
					elements.stream().map((e) -> (e.generateJavaFieldInitializationInConstructorSourceCode()))
							.filter(Objects::nonNull).collect(Collectors.toList()),
					"\n") + "\n");
			result.append("}" + "\n");
			result.append(MiscUtils.stringJoin(
					elements.stream().map((e) -> e.generateJavaFieldAccessorsSourceCode()).collect(Collectors.toList()),
					"\n") + "\n");
			result.append(
					MiscUtils.stringJoin(elements.stream().map((e) -> e.generateRequiredInnerJavaTypesSourceCode())
							.filter(Objects::nonNull).collect(Collectors.toList()), "\n") + "\n");
			result.append("}");
			return result.toString();
		}
	}

	public static interface Structured {

	}

	public static class EnumerationStructure extends Structure {
		private List<EnumerationItem> items = new ArrayList<EnumerationItem>();

		public List<EnumerationItem> getItems() {
			return items;
		}

		public void setItems(List<EnumerationItem> items) {
			this.items = items;
		}

		@Override
		public String generateJavaTypeSourceCode(String className) {
			return "public enum " + className + "{" + "\n"
					+ MiscUtils.stringJoin(items.stream().map((e) -> e.getName()).collect(Collectors.toList()), ", ")
					+ ";" + "\n" + "}";
		}
	}

	public static class EnumerationItem {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static abstract class Element {
		private String name;
		private Optionality optionality;
		private boolean multiple = false;

		protected abstract String getTypeName();

		protected abstract String generateRequiredInnerJavaTypesSourceCode();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Optionality getOptionality() {
			return optionality;
		}

		public void setOptionality(Optionality optionality) {
			this.optionality = optionality;
		}

		public boolean isMultiple() {
			return multiple;
		}

		public void setMultiple(boolean multiple) {
			this.multiple = multiple;
		}

		private String getFinalTypeName() {
			if (multiple) {
				return getTypeName() + "[]";
			} else {
				return getTypeName();
			}
		}

		protected String generateJavaFieldDeclarationSourceCode() {
			String defaultValueSettingString = "";
			if (getOptionality() != null) {
				if (getOptionality().getDefaultValueExpression() != null) {
					defaultValueSettingString = "=" + getOptionality().getDefaultValueExpression();
				}
			}
			return "private " + getFinalTypeName() + " " + getName() + defaultValueSettingString + ";";
		}

		protected String generateJavaConstructorParameterDeclarationSourceCode() {
			if (getOptionality() != null) {
				return null;
			}
			return getFinalTypeName() + " " + getName();
		}

		protected String generateJavaFieldInitializationInConstructorSourceCode() {
			if (getOptionality() != null) {
				return null;
			}
			return "this." + getName() + "=" + getName() + ";";
		}

		protected String generateJavaFieldAccessorsSourceCode() {
			StringBuilder result = new StringBuilder();
			result.append("public " + getFinalTypeName() + " get" + getName().substring(0, 1).toUpperCase()
					+ getName().substring(1) + "(){" + "\n");
			result.append("return " + getName() + ";" + "\n");
			result.append("}");
			if (getOptionality() != null) {
				result.append("\n" + "public void set" + getName().substring(0, 1).toUpperCase()
						+ getName().substring(1) + "(" + getFinalTypeName() + " " + getName() + "){" + "\n");
				result.append("this." + getName() + "=" + getName() + ";" + "\n");
				result.append("}");
			}
			return result.toString();
		}

		@Override
		public String toString() {
			if (optionality != null) {
				return name;
			} else {
				return "(" + name + ")";
			}
		}

	}

	public static class SimpleElement extends Element {
		private String typeName;

		@Override
		public String getTypeName() {
			return typeName;
		}

		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}

		@Override
		protected String generateRequiredInnerJavaTypesSourceCode() {
			return null;
		}

	}

	public static class StructuredElement extends Element {
		private Structure structure;

		public Structure getStructure() {
			return structure;
		}

		public void setStructure(Structure structure) {
			this.structure = structure;
		}

		@Override
		protected String generateRequiredInnerJavaTypesSourceCode() {
			return "static " + structure.generateJavaTypeSourceCode(getStructureClassName());
		}

		@Override
		protected String getTypeName() {
			return getStructureClassName();
		}

		private String getStructureClassName() {
			return getName().substring(0, 1).toUpperCase() + getName().substring(1) + "Structure";
		}
	}

	public static class Optionality {
		private String defaultValueExpression;

		public String getDefaultValueExpression() {
			return defaultValueExpression;
		}

		public void setDefaultValueExpression(String defaultValueExpression) {
			this.defaultValueExpression = defaultValueExpression;
		}
	}

}
