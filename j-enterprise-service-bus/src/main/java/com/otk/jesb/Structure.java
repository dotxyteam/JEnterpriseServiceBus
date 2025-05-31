package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.resource.builtin.SharedStructureModel;
import com.otk.jesb.solution.Reference;
import com.otk.jesb.util.MiscUtils;
import xy.reflect.ui.util.ClassUtils;

public abstract class Structure {

	public abstract String generateJavaTypeSourceCode(String className);

	public abstract void validate(boolean recursively) throws ValidationError;

	public abstract String toString();

	public static class ClassicStructure extends Structure {

		private List<Element> elements = new ArrayList<Element>();

		public List<Element> getElements() {
			return elements;
		}

		public void setElements(List<Element> elements) {
			this.elements = elements;
		}

		@Override
		public String generateJavaTypeSourceCode(String className) {
			StringBuilder result = new StringBuilder();
			if (MiscUtils.isPackageNameInClassName(className)) {
				result.append("package " + MiscUtils.extractPackageNameFromClassName(className) + ";" + "\n");
			}
			result.append("public class " + MiscUtils.extractSimpleNameFromClassName(className) + " implements "
					+ MiscUtils.adaptClassNameToSourceCode(Structured.class.getName()) + "{" + "\n");
			result.append(MiscUtils.stringJoin(elements.stream().map((e) -> e.generateJavaFieldDeclarationSourceCode())
					.collect(Collectors.toList()), "\n") + "\n");
			result.append(
					"public " + MiscUtils.extractSimpleNameFromClassName(className) + "("
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

		@Override
		public void validate(boolean recursively) throws ValidationError {
			if (elements.size() == 0) {
				throw new ValidationError("No declared element");
			}
			List<String> elementNames = new ArrayList<String>();
			for (Element element : elements) {
				if (elementNames.contains(element.getName())) {
					throw new ValidationError("Duplicate element name detected: '" + element.getName() + "'");
				} else {
					elementNames.add(element.getName());
				}
			}
			if (recursively) {
				for (Element element : elements) {
					element.validate(recursively);
				}
			}
		}

		@Override
		public String toString() {
			return "<ClassicStructure>";
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

		@Override
		public void validate(boolean recursively) throws ValidationError {
			if (items.size() == 0) {
				throw new ValidationError("No declared item");
			}
			List<String> itemNames = new ArrayList<String>();
			for (EnumerationItem item : items) {
				if (!MiscUtils.VARIABLE_NAME_PATTERN.matcher(item.getName()).matches()) {
					throw new ValidationError("Invalid element name: '" + item.getName()
							+ "' (should match the following regular expression: "
							+ MiscUtils.VARIABLE_NAME_PATTERN.pattern() + ")");
				}
				if (itemNames.contains(item.getName())) {
					throw new ValidationError("Duplicate item name detected: '" + item.getName() + "'");
				} else {
					itemNames.add(item.getName());
				}
			}
		}

		@Override
		public String toString() {
			return "<EnumerationStructure>";
		}

	}

	public static class SharedStructureReference extends Structure {

		private Reference<SharedStructureModel> modelReference = new Reference<SharedStructureModel>(
				SharedStructureModel.class, model -> {
					Reference<SharedStructureModel> newModelReference = Reference.get(model);
					try {
						checkNoReferenceCycle(newModelReference, false);
						return true;
					} catch (IllegalArgumentException e) {
						return false;
					}
				}, newPath -> {
					if (newPath != null) {
						Reference<SharedStructureModel> newModelReference = new Reference<SharedStructureModel>(
								SharedStructureModel.class);
						newModelReference.setPath(newPath);
						checkNoReferenceCycle(newModelReference, true);
					}
				}

		);

		public Reference<SharedStructureModel> getModelReference() {
			return modelReference;
		}

		public void setModelReference(Reference<SharedStructureModel> modelReference) {
			this.modelReference = modelReference;
		}

		private void checkNoReferenceCycle(Reference<SharedStructureModel> modelReference, boolean recursiveCheck) {
			if (modelReference != null) {
				SharedStructureModel model = modelReference.resolve();
				if (model != null) {
					Structure structure = model.getStructure();
					if (structure instanceof ClassicStructure) {
						checkNoReferenceCycle((ClassicStructure) structure, recursiveCheck);
					}
				}
			}
		}

		private void checkNoReferenceCycle(ClassicStructure classicStructure, boolean recursiveCheck) {
			for (Element element : classicStructure.getElements()) {
				if (element instanceof StructuredElement) {
					Structure subStructure = ((StructuredElement) element).getStructure();
					if (subStructure instanceof SharedStructureReference) {
						if (subStructure == SharedStructureReference.this) {
							throw new IllegalArgumentException("Shared structure reference cycle detected");
						}
						if (recursiveCheck) {
							checkNoReferenceCycle(((SharedStructureReference) subStructure).getModelReference(),
									recursiveCheck);
						}
					}
					if (subStructure instanceof ClassicStructure) {
						checkNoReferenceCycle((ClassicStructure) subStructure, recursiveCheck);
					}
				}
			}
		}

		public Class<? extends Structured> getStructuredClass() {
			if (modelReference == null) {
				return null;
			}
			SharedStructureModel model = modelReference.resolve();
			if (model == null) {
				return null;
			}
			return model.getStructuredClass();
		}

		@Override
		public String generateJavaTypeSourceCode(String className) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void validate(boolean recursively) throws ValidationError {
			if (modelReference == null) {
				throw new ValidationError("Shared structure model reference not set");
			}
			SharedStructureModel model = modelReference.resolve();
			if (model == null) {
				throw new ValidationError("Failed to resolve the shared structure model reference");
			}
		}

		@Override
		public String toString() {
			return "<SharedStructureReference>";
		}
	}

	public static class EnumerationItem {
		private String name = "ITEM";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static abstract class Element {
		private String name = "element";
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

		private String getFinalTypeNameAdaptedToSourceCode() {
			if (multiple) {
				return MiscUtils.adaptClassNameToSourceCode(getTypeName()) + "[]";
			} else {
				return MiscUtils.adaptClassNameToSourceCode(getTypeName());
			}
		}

		protected String generateJavaFieldDeclarationSourceCode() {
			String defaultValueSettingString = "";
			if (getOptionality() != null) {
				if (getOptionality().getDefaultValueExpression() != null) {
					defaultValueSettingString = "=" + getOptionality().getDefaultValueExpression();
				}
			}
			return "private " + getFinalTypeNameAdaptedToSourceCode() + " " + getName() + defaultValueSettingString
					+ ";";
		}

		protected String generateJavaConstructorParameterDeclarationSourceCode() {
			if (getOptionality() != null) {
				return null;
			}
			return getFinalTypeNameAdaptedToSourceCode() + " " + getName();
		}

		protected String generateJavaFieldInitializationInConstructorSourceCode() {
			if (getOptionality() != null) {
				return null;
			}
			return "this." + getName() + "=" + getName() + ";";
		}

		protected String generateJavaFieldAccessorsSourceCode() {
			StringBuilder result = new StringBuilder();
			result.append("public " + getFinalTypeNameAdaptedToSourceCode() + " get"
					+ getName().substring(0, 1).toUpperCase() + getName().substring(1) + "(){" + "\n");
			result.append("return " + getName() + ";" + "\n");
			result.append("}");
			if (getOptionality() != null) {
				result.append(
						"\n" + "public void set" + getName().substring(0, 1).toUpperCase() + getName().substring(1)
								+ "(" + getFinalTypeNameAdaptedToSourceCode() + " " + getName() + "){" + "\n");
				result.append("this." + getName() + "=" + getName() + ";" + "\n");
				result.append("}");
			}
			return result.toString();
		}

		public void validate(boolean recursively) throws ValidationError {
			if (!MiscUtils.VARIABLE_NAME_PATTERN.matcher(name).matches()) {
				throw new ValidationError(
						"Invalid element name: '" + name + "' (should match the following regular expression: "
								+ MiscUtils.VARIABLE_NAME_PATTERN.pattern() + ")");
			}
			if (optionality != null) {
				if (optionality.getDefaultValueExpression() != null) {
					try {
						MiscUtils.compileExpression(optionality.getDefaultValueExpression(), Collections.emptyList(),
								Object.class);
					} catch (CompilationError e) {
						throw new ValidationError("Invalid default value expression detected", e);
					}
				}
			}
		}

		@Override
		public String toString() {
			String result = name;
			if (optionality == null) {
				result = "(" + result + ")";
			}
			if (multiple) {
				result = result + "*";
			}
			if (optionality != null) {
				result = result + "?";
			}
			return result;
		}

	}

	public static class SimpleElement extends Element {

		private String typeName = getTypeNameOptions().get(0);

		@Override
		public String getTypeName() {
			return typeName;
		}

		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}

		public List<String> getTypeNameOptions() {
			List<String> result = new ArrayList<String>();
			result.add(String.class.getName());
			result.addAll(Arrays.asList(ClassUtils.PRIMITIVE_CLASSES).stream().map(cls -> cls.getName())
					.collect(Collectors.toList()));
			result.addAll(Arrays.asList(ClassUtils.PRIMITIVE_WRAPPER_CLASSES).stream().map(cls -> cls.getName())
					.collect(Collectors.toList()));
			return result;
		}

		@Override
		protected String generateRequiredInnerJavaTypesSourceCode() {
			return null;
		}

	}

	public static class StructuredElement extends Element {

		private Structure structure = new ClassicStructure();

		public Structure getStructure() {
			return structure;
		}

		public void setStructure(Structure structure) {
			this.structure = structure;
		}

		@Override
		protected String generateRequiredInnerJavaTypesSourceCode() {
			if (structure instanceof SharedStructureReference) {
				return "";
			}
			return "static " + structure.generateJavaTypeSourceCode(getStructuredClassName());
		}

		@Override
		protected String getTypeName() {
			return getStructuredClassName();
		}

		private String getStructuredClassName() {
			if (structure instanceof SharedStructureReference) {
				Class<? extends Structured> structuredClass = ((SharedStructureReference) structure)
						.getStructuredClass();
				return structuredClass.getName();
			}
			return getName().substring(0, 1).toUpperCase() + getName().substring(1) + "Structure";
		}

		public List<Element> getSubElements() {
			if (!(structure instanceof ClassicStructure)) {
				return null;
			}
			return ((ClassicStructure) structure).getElements();
		}

		public void setSubElements(List<Element> elements) {
			if (!(structure instanceof ClassicStructure)) {
				return;
			}
			((ClassicStructure) structure).setElements(elements);
		}

		@Override
		public void validate(boolean recursively) throws ValidationError {
			super.validate(recursively);
			if (recursively) {
				if (structure != null) {
					structure.validate(recursively);
				}
			}
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
