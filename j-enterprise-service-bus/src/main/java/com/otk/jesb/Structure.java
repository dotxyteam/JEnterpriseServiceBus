package com.otk.jesb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.meta.Date;
import com.otk.jesb.meta.DateTime;
import com.otk.jesb.resource.builtin.SharedStructureModel;
import com.otk.jesb.Reference;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.TreeVisitor;
import com.otk.jesb.util.TreeVisitor.VisitStatus;

import xy.reflect.ui.util.ClassUtils;

public abstract class Structure {

	public abstract String generateJavaTypeSourceCode(String className, String implemented, String extended,
			String afterFieldDeclarations, String additionalMethodDeclarations, Map<Object, Object> options);

	public abstract TreeVisitor.VisitStatus visitElements(TreeVisitor<Element> visitor);

	public abstract void validate(boolean recursively) throws ValidationError;

	public abstract String toString();

	public String generateJavaTypeSourceCode(String className) {
		return generateJavaTypeSourceCode(className, null, null, null, null, Collections.emptyMap());
	}

	public static class ClassicStructure extends Structure {

		private List<Element> elements = new ArrayList<Element>();

		public List<Element> getElements() {
			return elements;
		}

		public void setElements(List<Element> elements) {
			this.elements = elements;
		}

		@Override
		public String generateJavaTypeSourceCode(String className, String additionalyImplemented,
				String additionalyExtended, String afterFieldDeclarations, String afterMethodDeclarations,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			if (MiscUtils.isPackageNameInClassName(className)) {
				result.append("package " + MiscUtils.extractPackageNameFromClassName(className) + ";" + "\n");
			}
			String classSimpleName = MiscUtils.extractSimpleNameFromClassName(className);
			result.append("public class " + classSimpleName
					+ ((additionalyExtended != null) ? (" extends " + additionalyExtended) : "")
					+ ((additionalyImplemented != null) ? (" implements " + additionalyImplemented) : "") + "{" + "\n");
			result.append(MiscUtils.stringJoin(elements.stream()
					.map((e) -> e.generateJavaFieldDeclaration(className, options)).collect(Collectors.toList()), "\n")
					+ "\n");
			if (afterFieldDeclarations != null) {
				result.append(afterFieldDeclarations + "\n");
			}
			result.append(generateJavaConstructorSourceCode(className, options) + "\n");
			result.append(MiscUtils
					.stringJoin(elements.stream().map((e) -> e.generateJavaMethodDeclarations(className, options))
							.filter(Objects::nonNull).collect(Collectors.toList()), "\n")
					+ "\n");
			result.append(generateJavaToStringMethodSourceCode(className, options) + "\n");
			if (afterMethodDeclarations != null) {
				result.append(afterMethodDeclarations + "\n");
			}
			result.append(MiscUtils.stringJoin(
					elements.stream().map((e) -> e.generateRequiredInnerJavaTypesSourceCode(className, options))
							.filter(Objects::nonNull).collect(Collectors.toList()),
					"\n") + "\n");
			result.append("}");
			return result.toString();
		}

		protected String generateJavaToStringMethodSourceCode(String className, Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			result.append("@Override\n");
			result.append("public String toString() {\n");
			result.append(generateJavaToStringMethodBody(className, options) + "\n");
			result.append("}");
			return result.toString();
		}

		protected String generateJavaToStringMethodBody(String className, Map<Object, Object> options) {
			String classSimpleName = MiscUtils.extractSimpleNameFromClassName(className);
			return "return \"" + classSimpleName + " [" + elements.stream()
					.map((e) -> (e.getName() + "=\" + " + e.getName() + " + \"")).collect(Collectors.joining(", "))
					+ "]\";";
		}

		protected String generateJavaConstructorSourceCode(String className, Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			String classSimpleName = MiscUtils.extractSimpleNameFromClassName(className);
			result.append(
					"public " + classSimpleName + "("
							+ MiscUtils.stringJoin(
									elements.stream()
											.map((e) -> e.generateJavaConstructorParameterDeclaration(className,
													options))
											.filter(Objects::nonNull).collect(Collectors.toList()),
									", ")
							+ "){" + "\n");
			result.append(generateJavaConstructorBody(className, options) + "\n");
			result.append("}");
			return result.toString();
		}

		protected String generateJavaConstructorBody(String className, Map<Object, Object> options) {
			return MiscUtils.stringJoin(
					elements.stream().map((e) -> (e.generateJavaFieldConstructorStatement(className, options)))
							.filter(Objects::nonNull).collect(Collectors.toList()),
					"\n");
		}

		@Override
		public TreeVisitor.VisitStatus visitElements(TreeVisitor<Element> visitor) {
			return TreeVisitor.visitTreesFrom(elements, element -> element.visit(visitor),
					TreeVisitor.VisitStatus.VISIT_NOT_INTERRUPTED);
		}

		@Override
		public void validate(boolean recursively) throws ValidationError {
			if (elements.size() == 0) {
				throw new ValidationError("No declared element");
			}
			List<String> elementNames = new ArrayList<String>();
			for (Element element : elements) {
				if (elementNames.contains(element.getName())) {
					throw new ValidationError(
							"Duplicate name detected among child elements: '" + element.getName() + "'");
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

	public static class DerivedClassicStructure extends ClassicStructure {

		private String baseStructureTypeName;
		private Structure baseStructure;

		public DerivedClassicStructure(String baseStructureTypeName, Structure baseStructure) {
			this.baseStructureTypeName = baseStructureTypeName;
			this.baseStructure = baseStructure;
		}

		public Structure getBaseStructure() {
			return baseStructure;
		}

		public String getBaseStructureTypeName() {
			return baseStructureTypeName;
		}

		private List<Element> collectRecursivelyBaseStructureElements(Structure baseStructure) {
			if (baseStructure instanceof DerivedClassicStructure) {
				DerivedClassicStructure derivedStructure = (DerivedClassicStructure) baseStructure;
				List<Element> result = new ArrayList<Structure.Element>();
				result.addAll(collectRecursivelyBaseStructureElements(derivedStructure.getBaseStructure()));
				result.addAll(derivedStructure.getElements());
				return result;
			}
			if (baseStructure instanceof ClassicStructure) {
				ClassicStructure baseClassicStructure = (ClassicStructure) baseStructure;
				return baseClassicStructure.getElements();
			}
			if (baseStructure instanceof SharedStructureReference) {
				return collectRecursivelyBaseStructureElements(((SharedStructureReference) baseStructure).resolve());
			}
			return Collections.emptyList();
		}

		@Override
		protected String generateJavaConstructorSourceCode(String className, Map<Object, Object> options) {
			String classSimpleName = MiscUtils.extractSimpleNameFromClassName(className);
			String result = super.generateJavaConstructorSourceCode(className, options);
			List<Element> baseStructureElements = collectRecursivelyBaseStructureElements(baseStructure);
			if (baseStructureElements.size() > 0) {
				result = result.replace(classSimpleName + "(",
						classSimpleName + "("
								+ baseStructureElements.stream()
										.map(element -> element.generateJavaConstructorParameterDeclaration(
												baseStructureTypeName, options))
										.collect(Collectors.joining(", "))
								+ ", ");
			}
			return result;
		}

		@Override
		protected String generateJavaConstructorBody(String className, Map<Object, Object> options) {
			String result = super.generateJavaConstructorBody(className, options);
			List<Element> baseStructureElements = collectRecursivelyBaseStructureElements(baseStructure);
			if (baseStructureElements.size() > 0) {
				result = "super("
						+ baseStructureElements.stream().map(Element::getName).collect(Collectors.joining(", "))
						+ ");\n" + result;
			}
			return result;
		}

		@Override
		public String generateJavaTypeSourceCode(String className, String additionalyImplemented,
				String additionalyExtended, String afterFieldDeclarations, String afterMethodDeclarations,
				Map<Object, Object> options) {
			String classSimpleName = MiscUtils.extractSimpleNameFromClassName(className);
			return super.generateJavaTypeSourceCode(className, additionalyImplemented, additionalyExtended,
					afterFieldDeclarations, afterMethodDeclarations, options).replace("class " + classSimpleName,
							"class " + classSimpleName + " extends " + baseStructureTypeName);
		}

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
		public String generateJavaTypeSourceCode(String className, String additionalyImplemented,
				String additionalyExtended, String afterFieldDeclarations, String additionalMethodDeclarations,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			if (MiscUtils.isPackageNameInClassName(className)) {
				result.append("package " + MiscUtils.extractPackageNameFromClassName(className) + ";" + "\n");
			}
			result.append("public enum " + MiscUtils.extractSimpleNameFromClassName(className)
					+ ((additionalyExtended != null) ? (" extends " + additionalyExtended) : "")
					+ ((additionalyImplemented != null) ? (" implements " + additionalyImplemented) : "") + "{" + "\n");
			result.append(
					MiscUtils.stringJoin(items.stream().map((e) -> e.getName()).collect(Collectors.toList()), ", ")
							+ ";" + "\n");
			if (additionalMethodDeclarations != null) {
				result.append(additionalMethodDeclarations + "\n");
			}
			result.append("}");
			return result.toString();
		}

		@Override
		public VisitStatus visitElements(TreeVisitor<Element> visitor) {
			return VisitStatus.VISIT_NOT_INTERRUPTED;
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
					throw new ValidationError("Duplicate name detected among child items: '" + item.getName() + "'");
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
						checkNoReferenceCycleAndResolve(newModelReference, false);
						return true;
					} catch (IllegalArgumentException e) {
						return false;
					}
				}, newPath ->

				{
					if (newPath != null) {
						Reference<SharedStructureModel> newModelReference = new Reference<SharedStructureModel>(
								SharedStructureModel.class);
						newModelReference.setPath(newPath);
						checkNoReferenceCycleAndResolve(newModelReference, true);
					}
				});

		public Reference<SharedStructureModel> getModelReference() {
			return modelReference;
		}

		public void setModelReference(Reference<SharedStructureModel> modelReference) {
			this.modelReference = modelReference;
		}

		public Structure resolve() {
			return checkNoReferenceCycleAndResolve(modelReference, true);
		}

		private Structure checkNoReferenceCycleAndResolve(Reference<SharedStructureModel> modelReference,
				boolean recursiveCheck) {
			if (modelReference != null) {
				SharedStructureModel model = modelReference.resolve();
				if (model != null) {
					Structure modelStructure = model.getStructure();
					if (modelStructure instanceof SharedStructureReference) {
						if (modelStructure == SharedStructureReference.this) {
							throw new IllegalArgumentException("Shared structure reference cycle detected");
						}
						if (recursiveCheck) {
							return checkNoReferenceCycleAndResolve(
									((SharedStructureReference) modelStructure).getModelReference(), recursiveCheck);
						}
					}
					if (modelStructure instanceof ClassicStructure) {
						checkNoReferenceCycle((ClassicStructure) modelStructure, recursiveCheck);
					}
					return modelStructure;
				}
			}
			return null;
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
							checkNoReferenceCycleAndResolve(
									((SharedStructureReference) subStructure).getModelReference(), recursiveCheck);
						}
					}
					if (subStructure instanceof ClassicStructure) {
						checkNoReferenceCycle((ClassicStructure) subStructure, recursiveCheck);
					}
				}
			}
		}

		public Class<?> getStructuredClass() {
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
		public String generateJavaTypeSourceCode(String className, String additionalyImplemented,
				String additionalyExtended, String afterFieldDeclarations, String additionalMethodDeclarations,
				Map<Object, Object> options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public VisitStatus visitElements(TreeVisitor<Element> visitor) {
			Structure resolvedStructure = resolve();
			if (resolvedStructure == null) {
				return VisitStatus.VISIT_NOT_INTERRUPTED;
			}
			return resolvedStructure.visitElements(visitor);
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

		protected abstract String getTypeName(String parentClassName);

		protected abstract String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
				Map<Object, Object> options);

		protected abstract TreeVisitor.VisitStatus visit(TreeVisitor<Element> visitor);

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

		protected String getFinalTypeNameAdaptedToSourceCode(String parentClassName) {
			if (multiple) {
				return MiscUtils.adaptClassNameToSourceCode(getTypeName(parentClassName)) + "[]";
			} else {
				return MiscUtils.adaptClassNameToSourceCode(getTypeName(parentClassName));
			}
		}

		protected String generateJavaFieldDeclaration(String parentClassName, Map<Object, Object> options) {
			String result = ElementAccessMode.ACCESSORS.isSet(options) ? "private " : "public ";
			if (getOptionality() == null) {
				result += "final ";
			}
			result += getFinalTypeNameAdaptedToSourceCode(parentClassName) + " " + getName();
			if ((getOptionality() != null) && (getOptionality().getDefaultValueExpression() != null)) {
				result += "=" + getOptionality().getDefaultValueExpression();
			}
			result += ";";
			return result;
		}

		protected String generateJavaMethodDeclarations(String parentClassName, Map<Object, Object> options) {
			if (ElementAccessMode.ACCESSORS.isSet(options)) {
				StringBuilder result = new StringBuilder();
				String finalTypeName = getFinalTypeNameAdaptedToSourceCode(parentClassName);
				result.append("public " + finalTypeName + " get" + name.substring(0, 1).toUpperCase()
						+ name.substring(1) + "() {\n");
				result.append("return " + name + ";\n");
				result.append("}\n");
				if (getOptionality() != null) {
					result.append("public void set" + name.substring(0, 1).toUpperCase() + name.substring(1) + "("
							+ finalTypeName + " " + name + ") {\n");
					result.append("this." + name + " = " + name + ";\n");
					result.append("}\n");
				}
				return result.toString();
			} else {
				return null;
			}
		}

		protected String generateJavaConstructorParameterDeclaration(String parentClassName,
				Map<Object, Object> options) {
			if (getOptionality() != null) {
				return null;
			}
			return getFinalTypeNameAdaptedToSourceCode(parentClassName) + " " + getName();
		}

		protected String generateJavaFieldConstructorStatement(String parentClassName, Map<Object, Object> options) {
			if (getOptionality() != null) {
				return null;
			}
			return "this." + getName() + "=" + getName() + ";";
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

	public static class ElementProxy extends Element {
		private Element base;

		public ElementProxy(Element base) {
			this.base = base;
		}

		public String getName() {
			return base.getName();
		}

		public void setName(String name) {
			base.setName(name);
		}

		public Optionality getOptionality() {
			return base.getOptionality();
		}

		public void setOptionality(Optionality optionality) {
			base.setOptionality(optionality);
		}

		public boolean isMultiple() {
			return base.isMultiple();
		}

		public void setMultiple(boolean multiple) {
			base.setMultiple(multiple);
		}

		@Override
		protected String getTypeName(String parentClassName) {
			return base.getTypeName(parentClassName);
		}

		@Override
		protected String getFinalTypeNameAdaptedToSourceCode(String parentClassName) {
			return base.getFinalTypeNameAdaptedToSourceCode(parentClassName);
		}

		@Override
		protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName, Map<Object, Object> options) {
			return base.generateRequiredInnerJavaTypesSourceCode(parentClassName, options);
		}

		@Override
		protected String generateJavaFieldDeclaration(String parentClassName, Map<Object, Object> options) {
			return base.generateJavaFieldDeclaration(parentClassName, options);
		}

		@Override
		protected String generateJavaMethodDeclarations(String parentClassName, Map<Object, Object> options) {
			return base.generateJavaMethodDeclarations(parentClassName, options);
		}

		@Override
		protected String generateJavaConstructorParameterDeclaration(String parentClassName,
				Map<Object, Object> options) {
			return base.generateJavaConstructorParameterDeclaration(parentClassName, options);
		}

		@Override
		protected String generateJavaFieldConstructorStatement(String parentClassName, Map<Object, Object> options) {
			return base.generateJavaFieldConstructorStatement(parentClassName, options);
		}

		@Override
		protected VisitStatus visit(TreeVisitor<Element> visitor) {
			return base.visit(visitor);
		}

		public void validate(boolean recursively) throws ValidationError {
			base.validate(recursively);
		}

		@Override
		public String toString() {
			return "ElementProxy [base=" + base + "]";
		}

	}

	public static class SimpleElement extends Element {

		public static final Map<String, String> TYPE_NAME_BY_ALIAS = Collections
				.unmodifiableMap(new HashMap<String, String>() {
					private static final long serialVersionUID = 1L;
					{
						put("<binary>", byte[].class.getName());
					}
				});

		private String typeNameOrAlias = getTypeNameOrAliasOptions().get(0);

		public String getTypeNameOrAlias() {
			return typeNameOrAlias;
		}

		public void setTypeNameOrAlias(String typeNameOrAlias) {
			this.typeNameOrAlias = typeNameOrAlias;
		}

		@Override
		public String getTypeName(String parentClassName) {
			return TYPE_NAME_BY_ALIAS.getOrDefault(typeNameOrAlias, typeNameOrAlias);
		}

		public List<String> getTypeNameOrAliasOptions() {
			List<String> result = new ArrayList<String>();
			result.add(String.class.getName());
			result.addAll(Arrays.asList(ClassUtils.PRIMITIVE_CLASSES).stream().map(cls -> cls.getName())
					.collect(Collectors.toList()));
			result.addAll(Arrays.asList(ClassUtils.PRIMITIVE_WRAPPER_CLASSES).stream().map(cls -> cls.getName())
					.collect(Collectors.toList()));
			result.add(Date.class.getName());
			result.add(DateTime.class.getName());
			result.addAll(TYPE_NAME_BY_ALIAS.keySet());
			return result;
		}

		@Override
		protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName, Map<Object, Object> options) {
			return null;
		}

		@Override
		protected VisitStatus visit(TreeVisitor<Element> visitor) {
			return visitor.visitNode(this);
		}

		@Override
		public void validate(boolean recursively) throws ValidationError {
			super.validate(recursively);
			if ((typeNameOrAlias == null) || typeNameOrAlias.isEmpty()) {
				throw new ValidationError("Type name not provided");
			}
			try {
				MiscUtils.getJESBClass(TYPE_NAME_BY_ALIAS.getOrDefault(typeNameOrAlias, typeNameOrAlias));
			} catch (Exception e) {
				throw new ValidationError("Failed to validate the type name: '" + typeNameOrAlias + "'", e);
			}
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
		protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName, Map<Object, Object> options) {
			if (structure instanceof SharedStructureReference) {
				return "";
			}
			String className = getStructuredClassName(parentClassName);
			return "static " + structure.generateJavaTypeSourceCode(className, null, null, null, null, options);
		}

		@Override
		protected String getTypeName(String parentClassName) {
			return getStructuredClassName(parentClassName);
		}

		protected String getStructuredClassName(String parentClassName) {
			if (structure instanceof SharedStructureReference) {
				Class<?> structuredClass = ((SharedStructureReference) structure).getStructuredClass();
				return structuredClass.getName();
			}
			boolean parentClassInner = !MiscUtils.isPackageNameInClassName(parentClassName);
			/*
			 * The Java language requires nested classes to have a name different from that
			 * of all enclosing classes, regardless of the depth of the nesting. This code
			 * detects nesting and adds a prefix to comply with this constraint.
			 */
			String prefix = (parentClassInner ? parentClassName : "");
			return prefix + getName().substring(0, 1).toUpperCase() + getName().substring(1) + "Structure";
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
		protected VisitStatus visit(TreeVisitor<Element> visitor) {
			VisitStatus result = visitor.visitNode(this);
			if (structure != null) {
				result = TreeVisitor.combine(result, structure.visitElements(visitor));
			}
			return result;
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

	private interface CodeGenerationOption {

		public static boolean isSet(CodeGenerationOption option, Map<Object, Object> options) {
			return option.equals(options.get(option.getClass()));
		}

		public static void set(CodeGenerationOption option, Map<Object, Object> options) {
			options.put(option.getClass(), option);
		}

		public static Map<Object, Object> singleton(CodeGenerationOption option) {
			return Collections.singletonMap(option.getClass(), option);
		}
	}

	public enum ElementAccessMode implements CodeGenerationOption {
		PUBLIC_FIELD, ACCESSORS;

		public boolean isSet(Map<Object, Object> options) {
			return CodeGenerationOption.isSet(this, options);
		}

		public void set(Map<Object, Object> options) {
			CodeGenerationOption.set(this, options);
		}

		public Map<Object, Object> singleton() {
			return CodeGenerationOption.singleton(this);
		}
	}

}
