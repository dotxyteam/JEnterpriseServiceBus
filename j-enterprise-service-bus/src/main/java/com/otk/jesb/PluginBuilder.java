package com.otk.jesb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.otk.jesb.Structure.Element;
import com.otk.jesb.Structure.ElementProxy;
import com.otk.jesb.Structure.EnumerationStructure;
import com.otk.jesb.Structure.SharedStructureReference;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.Structure.StructuredElement;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.operation.ParameterBuilder;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.solution.Folder;
import com.otk.jesb.solution.JAR;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.ui.JESBReflectionUI;
import com.otk.jesb.ui.JESBReflectionUI.VariantCustomizations;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.TreeVisitor;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.util.ClassUtils;
import xy.reflect.ui.util.ReflectionUIUtils;

public class PluginBuilder {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				GUI.INSTANCE.openObjectFrame(PluginBuilder.INSTANCE);
			}
		});
	}

	public static final PluginBuilder INSTANCE = new PluginBuilder();

	private String packageName;
	private List<ResourceDescriptor> resources = new ArrayList<ResourceDescriptor>();
	private List<OperationDescriptor> operations = new ArrayList<OperationDescriptor>();
	private List<ActivatorDescriptor> activators = new ArrayList<ActivatorDescriptor>();

	private JAR onlineJAR;

	private PluginBuilder() {
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public List<OperationDescriptor> getOperations() {
		return operations;
	}

	public void setOperations(List<OperationDescriptor> operations) {
		this.operations = operations;
	}

	public List<ResourceDescriptor> getResources() {
		return resources;
	}

	public void setResources(List<ResourceDescriptor> resources) {
		this.resources = resources;
	}

	public List<ActivatorDescriptor> getActivators() {
		return activators;
	}

	public void setActivators(List<ActivatorDescriptor> activators) {
		this.activators = activators;
	}

	public void generateProject(File outputDirectory) throws IOException {
		MiscUtils.delete(outputDirectory);
		File sourceDirectroy = getSourceDirectory(outputDirectory);
		MiscUtils.createDirectory(sourceDirectroy, true);
		generateSources(sourceDirectroy);
		File resourceDirectroy = getResourceDirectory(outputDirectory);
		MiscUtils.createDirectory(resourceDirectroy, true);
		generateResources(resourceDirectroy);
	}

	public void generateJAR(File jarFile) throws Exception {
		File temporaryDirectory = MiscUtils.createTemporaryDirectory();
		try {
			generateProject(temporaryDirectory);
			List<Class<?>> classes = new ArrayList<Class<?>>();
			classes.addAll(MiscUtils.IN_MEMORY_COMPILER.compile(getSourceDirectory(temporaryDirectory)));
			classes = MiscUtils.expandWithEnclosedClasses(classes);
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
			manifest.getMainAttributes().put(JAR.PLUGIN_OPERATION_METADATA_CLASSES_MANIFEST_KEY,
					classes.stream().filter(clazz -> OperationMetadata.class.isAssignableFrom(clazz))
							.map(Class::getName).collect(Collectors.joining(",")));
			manifest.getMainAttributes().put(JAR.PLUGIN_ACTIVATOR_METADATA_CLASSES_MANIFEST_KEY,
					classes.stream().filter(clazz -> ActivatorMetadata.class.isAssignableFrom(clazz))
							.map(Class::getName).collect(Collectors.joining(",")));
			manifest.getMainAttributes().put(JAR.PLUGIN_RESOURCE_METADATA_CLASSES_MANIFEST_KEY,
					classes.stream().filter(clazz -> ResourceMetadata.class.isAssignableFrom(clazz)).map(Class::getName)
							.collect(Collectors.joining(",")));
			try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
				for (Class<?> clazz : classes) {
					String entryName = clazz.getName().replace(".", "/") + ".class";
					JarEntry jarEntry = new JarEntry(entryName);
					jarOutputStream.putNextEntry(jarEntry);
					byte[] classBinary = MiscUtils.IN_MEMORY_COMPILER.getClassBinary(clazz);
					if (classBinary == null) {
						throw new UnexpectedError();
					}
					jarOutputStream.write(classBinary);
					jarOutputStream.closeEntry();
				}
				File resourceDirectory = getResourceDirectory(temporaryDirectory);
				Files.walkFileTree(resourceDirectory.toPath(), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
						String entryName = resourceDirectory.toPath().relativize(filePath).toString();
						JarEntry jarEntry = new JarEntry(entryName);
						jarOutputStream.putNextEntry(jarEntry);
						jarOutputStream.write(MiscUtils.readBinary(filePath.toFile()));
						jarOutputStream.closeEntry();
						return super.visitFile(filePath, attrs);
					}
				});
			}
		} finally {
			MiscUtils.delete(temporaryDirectory);
		}
	}

	public void prepareTesting() throws Exception {
		unprepareTesting();
		File temporaryJarFile = MiscUtils.createTemporaryFile("jar");
		try {
			generateJAR(temporaryJarFile);
			onlineJAR = new JAR(temporaryJarFile);
			Solution.INSTANCE.setRequiredJARs(MiscUtils.added(Solution.INSTANCE.getRequiredJARs(),
					Solution.INSTANCE.getRequiredJARs().size(), onlineJAR));
		} finally {
			MiscUtils.delete(temporaryJarFile);
		}
	}

	public void unprepareTesting() throws Exception {
		if (onlineJAR != null) {
			Solution.INSTANCE.setRequiredJARs(MiscUtils.removed(Solution.INSTANCE.getRequiredJARs(), -1, onlineJAR));
			onlineJAR = null;
		}
	}

	private File getResourceDirectory(File projectDirectory) {
		return new File(projectDirectory, "src/main/resources");
	}

	private File getSourceDirectory(File projectDirectory) {
		return new File(projectDirectory, "src/main/java");
	}

	private void generateSources(File sourceDirectroy) {
		for (OperationDescriptor operation : operations) {
			operation.generateJavaSourceCode(sourceDirectroy, packageName);
		}
		for (ActivatorDescriptor activator : activators) {
			activator.generateJavaSourceCode(sourceDirectroy, packageName);
		}
		for (ResourceDescriptor resource : resources) {
			resource.generateJavaSourceCode(sourceDirectroy, packageName);
		}
	}

	private void generateResources(File resourceDirectroy) {
		for (OperationDescriptor operation : operations) {
			produceIcon(resourceDirectroy, operation.getOpertionTypeName(), operation.getIconImage());
		}
		for (ActivatorDescriptor activator : activators) {
			produceIcon(resourceDirectroy, activator.getActivatorTypeName(), activator.getIconImage());
		}
		for (ResourceDescriptor resource : resources) {
			produceIcon(resourceDirectroy, resource.getResourceTypeName(), resource.getIconImage());
		}
	}

	private void produceIcon(File resourceDirectroy, String typeName, BufferedImage iconImage) {
		try {
			File iconFile = new File(resourceDirectroy, packageName.replace(".", "/") + "/" + typeName + ".png");
			if (!iconFile.getParentFile().exists()) {
				MiscUtils.createDirectory(iconFile.getParentFile(), true);
			}
			if (iconImage != null) {
				ImageIO.write(iconImage, "png", iconFile);
			} else {
				MiscUtils.writeBinary(iconFile, MiscUtils.readBinary(JESB.class.getResourceAsStream("generic.png")),
						false);
			}
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
	}

	public void save(File file) throws IOException {
		try (FileOutputStream output = new FileOutputStream(file)) {
			MiscUtils.serialize(this, output);
		}
	}

	public void load(File file) throws IOException {
		try (FileInputStream input = new FileInputStream(file)) {
			PluginBuilder loaded = (PluginBuilder) MiscUtils.deserialize(input);
			this.packageName = loaded.packageName;
			this.operations = loaded.operations;
			this.activators = loaded.activators;
			this.resources = loaded.resources;
		}
	}

	public void validate() throws ValidationError {
		if ((packageName == null) || packageName.isEmpty()) {
			throw new ValidationError("Package name not provided");
		}
		for (OperationDescriptor operation : operations) {
			operation.validate();
		}
		for (ActivatorDescriptor activator : activators) {
			activator.validate();
		}
		for (ResourceDescriptor resource : resources) {
			resource.validate();
		}
	}

	private static void validateStructure(Structure structure) throws ValidationError {
		try {
			structure.visitElements(new TreeVisitor<Structure.Element>() {
				@Override
				public VisitStatus visitNode(Element element) {
					if (element instanceof StructuredElement) {
						if (((StructuredElement) element).getStructure() instanceof SharedStructureReference) {
							throw new IllegalStateException("Shared structure reference not allowed here");
						}
					}
					return VisitStatus.VISIT_NOT_INTERRUPTED;
				}
			});
		} catch (IllegalStateException e) {
			throw new ValidationError(e.getMessage());
		}
	}

	private static Element ensureNotReadOnly(Element element) {
		if (element.getOptionality() == null) {
			element = new Structure.ElementProxy(element) {

				String GHOST_DEFAULT_VALUE_EXPRESSION_TO_ENABLE_SETTER = "<" + MiscUtils.getDigitalUniqueIdentifier()
						+ ">";
				{
					Structure.Optionality optionality = new Structure.Optionality();
					optionality.setDefaultValueExpression(GHOST_DEFAULT_VALUE_EXPRESSION_TO_ENABLE_SETTER);
					setOptionality(optionality);
				}

				@Override
				protected String generateJavaFieldDeclaration(String parentClassName, Map<Object, Object> options) {
					return super.generateJavaFieldDeclaration(parentClassName, options)
							.replaceAll("\\s*=\\s*" + GHOST_DEFAULT_VALUE_EXPRESSION_TO_ENABLE_SETTER, "");
				}

			};
		}
		return element;
	}

	public static class OperationDescriptor {

		private static final String RESULT_OPTION_NAME = "result";

		private String opertionTypeName;
		private String opertionTypeCaption;
		private String categoryName;
		private byte[] operationIconImageData;
		private List<ParameterDescriptor> parameters = new ArrayList<ParameterDescriptor>();
		private ClassOptionDescriptor result;
		private String executionMethodBody;

		public String getOpertionTypeName() {
			return opertionTypeName;
		}

		public void setOpertionTypeName(String opertionTypeName) {
			this.opertionTypeName = opertionTypeName;
		}

		public String getOpertionTypeCaption() {
			return opertionTypeCaption;
		}

		public void setOpertionTypeCaption(String opertionTypeCaption) {
			this.opertionTypeCaption = opertionTypeCaption;
		}

		public String getCategoryName() {
			return categoryName;
		}

		public void setCategoryName(String categoryName) {
			this.categoryName = categoryName;
		}

		public List<ParameterDescriptor> getParameters() {
			return parameters;
		}

		public void setParameters(List<ParameterDescriptor> parameters) {
			this.parameters = parameters;
		}

		public ClassOptionDescriptor getResult() {
			return result;
		}

		public void setResult(ClassOptionDescriptor result) {
			this.result = result;
		}

		public String getExecutionMethodBody() {
			return executionMethodBody;
		}

		public void setExecutionMethodBody(String executionMethodBody) {
			this.executionMethodBody = executionMethodBody;
		}

		public void loadIconImage(File file) throws IOException {
			operationIconImageData = MiscUtils.readBinary(file);
		}

		public BufferedImage getIconImage() {
			if (operationIconImageData == null) {
				return null;
			}
			try {
				return ImageIO.read(new ByteArrayInputStream(operationIconImageData));
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
		}

		public File generateJavaSourceCode(File sourceDirectroy, String packageName) {
			String operationClassName = packageName + "." + opertionTypeName;
			String implemented = Operation.class.getName();
			Structure.ClassicStructure operationStructure = new Structure.ClassicStructure();
			Map<Object, Object> codeGenerationOptions = Structure.ElementAccessMode.ACCESSORS.singleton();
			for (ParameterDescriptor parameter : parameters) {
				operationStructure.getElements().add(parameter.getOperationClassElement(operationClassName));
			}
			StringBuilder additionalDeclarations = new StringBuilder();
			additionalDeclarations
					.append(generateExecutionMethodSourceCode(operationClassName, codeGenerationOptions) + "\n");
			additionalDeclarations
					.append(generateOperationBuilderClassSourceCode(operationClassName, codeGenerationOptions) + "\n");
			additionalDeclarations
					.append(generateMetadataClassSourceCode(operationClassName, codeGenerationOptions) + "\n");
			if (result != null) {
				additionalDeclarations.append(
						result.generateClassesSourceCode(operationClassName, RESULT_OPTION_NAME, codeGenerationOptions)
								+ "\n");
			}
			File javaFile = new File(sourceDirectroy, operationClassName.replace(".", "/") + ".java");
			try {
				if (!javaFile.getParentFile().exists()) {
					MiscUtils.createDirectory(javaFile.getParentFile(), true);
				}
				MiscUtils.write(javaFile, operationStructure.generateJavaTypeSourceCode(operationClassName, implemented,
						null, additionalDeclarations.toString(), codeGenerationOptions), false);
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			return javaFile;
		}

		protected String generateExecutionMethodSourceCode(String className,
				Map<Object, Object> codeGenerationOptions) {
			StringBuilder result = new StringBuilder();
			result.append("@Override\n");
			result.append("public Object execute() throws Throwable {\n");
			result.append(executionMethodBody + "\n");
			result.append("}");
			return result.toString();
		}

		protected String generateOperationBuilderClassSourceCode(String operationClassName,
				Map<Object, Object> options) {
			String operationClassSimpleName = MiscUtils.extractSimpleNameFromClassName(operationClassName);
			String implemented = OperationBuilder.class.getName() + "<" + operationClassSimpleName + ">";
			Structure.ClassicStructure operationBuilderStructure = new Structure.ClassicStructure();
			for (ParameterDescriptor parameter : parameters) {
				operationBuilderStructure.getElements()
						.add(parameter.getOperationBuilderClassElement(operationClassName));
			}
			StringBuilder additionalDeclarations = new StringBuilder();
			additionalDeclarations.append(generateOperationBuildMethodSourceCode(operationClassName, options));
			additionalDeclarations.append(generateOperationResultClassMethodSourceCode(operationClassName, options));
			additionalDeclarations
					.append(generateOperationBuilderValidationMethodSourceCode(operationClassName, options));
			additionalDeclarations.append(generateUICustomizationsMethodSourceCode(operationClassName, options) + "\n");
			return "static "
					+ operationBuilderStructure.generateJavaTypeSourceCode(getOperationBuilderClassSimpleName(),
							implemented, null, additionalDeclarations.toString(), options);
		}

		protected String generateUICustomizationsMethodSourceCode(String operationClassName,
				Map<Object, Object> codeGenerationOptions) {
			StringBuilder result = new StringBuilder();
			String builderClassName = operationClassName + "." + getOperationBuilderClassSimpleName();
			result.append("public static void " + GUI.UI_CUSTOMIZATIONS_METHOD_NAME + "("
					+ InfoCustomizations.class.getName() + " infoCustomizations) {\n");
			result.append("/* " + builderClassName + " form customization */\n");
			{
				result.append("{\n");
				result.append("/* field control positions */\n");
				result.append("" + InfoCustomizations.class.getName() + ".getTypeCustomization(infoCustomizations, "
						+ builderClassName + ".class.getName()).setCustomFieldsOrder(" + Arrays.class.getName()
						+ ".asList("
						+ parameters.stream()
								.map(parameter -> '"'
										+ parameter.getOperationBuilderClassElement(operationClassName).getName() + '"')
								.collect(Collectors.joining(", "))
						+ "));\n");
				for (ParameterDescriptor parameter : parameters) {
					Element element = parameter.getOperationBuilderClassElement(operationClassName);
					result.append("/* " + element.getName() + " control customization */\n");
					result.append("{\n");
					if (parameter.getCaption() != null) {
						result.append("" + InfoCustomizations.class.getName()
								+ ".getFieldCustomization(infoCustomizations, " + builderClassName
								+ ".class.getName(), \"" + element.getName() + "\").setCustomFieldCaption(\""
								+ MiscUtils.escapeJavaString(parameter.getCaption()) + "\");\n");
					}
					parameter.getNature().generateUICustomizationStatements(result, "infoCustomizations",
							operationClassName, getOperationBuilderClassSimpleName(), parameter.getName());
					result.append("}\n");
				}
				result.append("/* hide UI customization method */\n");
				result.append(InfoCustomizations.class.getName() + ".getMethodCustomization(infoCustomizations, "
						+ builderClassName + ".class.getName(), " + ReflectionUIUtils.class.getName()
						+ ".buildMethodSignature(\"void\", \"" + GUI.UI_CUSTOMIZATIONS_METHOD_NAME + "\", "
						+ Arrays.class.getName() + ".asList(" + InfoCustomizations.class.getName()
						+ ".class.getName()))).setHidden(true);\n");
				result.append("/* hide 'build(...)' method */\n");
				result.append(InfoCustomizations.class.getName() + ".getMethodCustomization(infoCustomizations, "
						+ builderClassName + ".class.getName(), " + ReflectionUIUtils.class.getName()
						+ ".buildMethodSignature(" + operationClassName + ".class.getName(), \"build\", "
						+ Arrays.class.getName() + ".asList("
						+ MiscUtils.adaptClassNameToSourceCode(ExecutionContext.class.getName()) + ".class.getName(), "
						+ MiscUtils.adaptClassNameToSourceCode(ExecutionInspector.class.getName())
						+ ".class.getName()))).setHidden(true);\n");
				result.append("}\n");
			}
			result.append("}");
			return result.toString();
		}

		protected String generateOperationBuilderValidationMethodSourceCode(String operationClassName,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			result.append("@Override\n");
			result.append("public void validate(boolean recursively, " + Plan.class.getName() + " currentPlan, "
					+ Step.class.getName() + " currentStep) {\n");
			result.append(generateValidationMethodBody(operationClassName, options) + "\n");
			result.append("}\n");
			return result.toString();
		}

		protected String generateOperationResultClassMethodSourceCode(String operationClassName,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			result.append("@Override\n");
			result.append("public Class<?> getOperationResultClass(" + Plan.class.getName() + " currentPlan, "
					+ Step.class.getName() + " currentStep) {\n");
			result.append(generateResultClassMethodBody(operationClassName, options) + "\n");
			result.append("}\n");
			return result.toString();
		}

		protected String generateOperationBuildMethodSourceCode(String operationClassName,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			String operationClassSimpleName = MiscUtils.extractSimpleNameFromClassName(operationClassName);
			result.append("@Override\n");
			result.append("public " + operationClassSimpleName + " build("
					+ MiscUtils.adaptClassNameToSourceCode(ExecutionContext.class.getName()) + " context, "
					+ MiscUtils.adaptClassNameToSourceCode(ExecutionInspector.class.getName())
					+ " executionInspector) throws Exception {\n");
			result.append(generateBuildMethodBody(operationClassName, options) + "\n");
			result.append("}\n");
			return result.toString();
		}

		protected String getOperationBuilderClassSimpleName() {
			return "Builder";
		}

		protected String generateBuildMethodBody(String operationClassName, Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			for (ParameterDescriptor parameter : parameters) {
				String buildStatementTarget = parameter.getOperationClassElement(operationClassName)
						.getFinalTypeNameAdaptedToSourceCode(operationClassName) + " " + parameter.getName();
				result.append(
						parameter.generateBuildStatement(buildStatementTarget, operationClassName, options) + "\n");
			}
			result.append("return new " + opertionTypeName + "("
					+ parameters.stream().map(ParameterDescriptor::getName).collect(Collectors.joining(", ")) + ");");
			return result.toString();
		}

		protected String generateMetadataClassSourceCode(String operationClassName, Map<Object, Object> options) {
			String className = "Metadata";
			String operationClassSimpleName = MiscUtils.extractSimpleNameFromClassName(operationClassName);
			String implemented = OperationMetadata.class.getName() + "<" + operationClassSimpleName + ">";
			StringBuilder result = new StringBuilder();
			result.append("public static class " + className + " implements " + implemented + "{" + "\n");
			{
				result.append("@Override\n");
				result.append("public String getOperationTypeName() {\n");
				result.append("return \"" + opertionTypeCaption + "\";" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public String getCategoryName() {\n");
				result.append("return \"" + categoryName + "\";" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public Class<? extends " + OperationBuilder.class.getName() + "<"
						+ operationClassSimpleName + ">> getOperationBuilderClass() {\n");
				result.append("return Builder.class;" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public " + ResourcePath.class.getName() + " getOperationIconImagePath() {\n");
				result.append("return new " + ResourcePath.class.getName() + "(" + ResourcePath.class.getName()
						+ ".specifyClassPathResourceLocation(" + operationClassSimpleName
						+ ".class.getName().replace(\".\", \"/\") + \".png\"));" + "\n");
				result.append("}\n");
			}
			result.append("}");
			return result.toString();
		}

		protected String generateResultClassMethodBody(String operationClassName, Map<Object, Object> options) {
			if (result == null) {
				return "return null;";
			}
			return result.generateClassOptionMethodBody(operationClassName, RESULT_OPTION_NAME, options);
		}

		protected String generateValidationMethodBody(String operationClassName, Map<Object, Object> options) {
			return "";
		}

		public void validate() throws ValidationError {
			if ((opertionTypeName == null) || opertionTypeName.isEmpty()) {
				throw new ValidationError("Operation class name not provided");
			}
			for (ParameterDescriptor parameter : parameters) {
				parameter.validate();
			}
		}

		public com.otk.jesb.operation.Experiment test() throws Exception {
			PluginBuilder.INSTANCE.prepareTesting();
			String fullClassName = PluginBuilder.INSTANCE.getPackageName() + "." + opertionTypeName;
			OperationBuilder<?> operationBuilder = (OperationBuilder<?>) MiscUtils
					.getJESBClass(fullClassName + "$Builder").newInstance();
			return new com.otk.jesb.operation.Experiment(operationBuilder);
		}

	}

	public static class ParameterDescriptor {

		private String name;
		private String caption;
		private ParameterNature nature = new SimpleParameterNature();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCaption() {
			return caption;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public ParameterNature getNature() {
			return nature;
		}

		public void setNature(ParameterNature nature) {
			this.nature = nature;
		}

		public Element getOperationClassElement(String operationClassName) {
			Element result = nature.getOperationClassElement(operationClassName, name);
			return result;
		}

		public Element getOperationBuilderClassElement(String operationClassName) {
			Element result = nature.getOperationBuilderClassElement(operationClassName, name);
			result = new Structure.ElementProxy(result) {

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String operationBuilderClassName,
						Map<Object, Object> options) {
					return nature.generateBuilderRequiredInnerJavaTypesSourceCode(operationClassName, name, options);
				}

			};
			result = ensureNotReadOnly(result);
			return result;
		}

		public String generateBuildStatement(String targetVariableName, String operationClassName,
				Map<Object, Object> options) {
			return targetVariableName + " = " + nature.generateBuildExpression(operationClassName, name, options) + ";";
		}

		public void validate() throws ValidationError {
			if ((name == null) || name.isEmpty()) {
				throw new ValidationError("Parameter name not provided");
			}
			nature.validate();
		}

	}

	public static class ClassOptionDescriptor {

		private Structure structure = new Structure.ClassicStructure();
		private List<StructureDerivationAlternative> concreteStructureAlternatives;

		public Structure getStructure() {
			return structure;
		}

		public void setStructure(Structure structure) {
			this.structure = structure;
		}

		public List<StructureDerivationAlternative> getConcreteStructureAlternatives() {
			return concreteStructureAlternatives;
		}

		public void setConcreteStructureAlternatives(
				List<StructureDerivationAlternative> concreteStructureAlternatives) {
			this.concreteStructureAlternatives = concreteStructureAlternatives;
		}

		public String generateClassesSourceCode(String parentClassName, String optionName,
				Map<Object, Object> options) {
			return getOptionElementUtility(parentClassName, optionName)
					.generateRequiredInnerJavaTypesSourceCode(parentClassName, options);
		}

		protected String generateClassOptionMethodBody(String parentClassName, String optionName,
				Map<Object, Object> options) {
			if (concreteStructureAlternatives != null) {
				return getOptionNatureUtility(parentClassName)
						.generateConcreteTypeNameGetterBody(parentClassName, optionName, options)
						.replace("class.getName()", "class");
			} else {
				return "return " + getOptionElementUtility(parentClassName, optionName)
						.getFinalTypeNameAdaptedToSourceCode(parentClassName) + ".class;";
			}
		}

		private Structure.Element getOptionElementUtility(String parentClassName, String optionName) {
			return getOptionNatureUtility(parentClassName).getOperationClassElement(parentClassName, optionName);
		}

		private DynamicParameterNature getOptionNatureUtility(String parentClassName) {
			return new DynamicParameterNature() {
				{
					setStructure(ClassOptionDescriptor.this.structure);
					setConcreteStructureAlternatives(ClassOptionDescriptor.this.concreteStructureAlternatives);
				}
			};
		}

		public void validate() throws ValidationError {
			structure.validate(true);
			PluginBuilder.validateStructure(structure);
			if (concreteStructureAlternatives != null) {
				for (StructureDerivationAlternative alternative : concreteStructureAlternatives) {
					alternative.validate();
				}
			}
		}
	}

	public static abstract class ParameterNature {

		protected abstract Element getOperationClassElement(String operationClassName, String parameterName);

		protected abstract Element getOperationBuilderClassElement(String operationClassName, String parameterName);

		protected abstract String generateBuilderRequiredInnerJavaTypesSourceCode(String operationClassName,
				String parameterName, Map<Object, Object> options);

		protected abstract String generateBuildExpression(String operationClassName, String parameterName,
				Map<Object, Object> options);

		protected abstract void generateUICustomizationStatements(StringBuilder result,
				String uiCustomizationsVariableName, String operationClassName, String operationBuilderClassSimpleName,
				String parameterName);

		public abstract void validate() throws ValidationError;

	}

	public static class SimpleParameterNature extends ParameterNature {

		private SimpleElement internalElement = new SimpleElement();
		private String defaultValueExpression;
		private boolean variant = false;
		private boolean nullable = false;

		public String getTypeNameOrAlias() {
			return internalElement.getTypeNameOrAlias();
		}

		public void setTypeNameOrAlias(String typeNameOrAlias) {
			internalElement.setTypeNameOrAlias(typeNameOrAlias);
		}

		public List<String> getTypeNameOrAliasOptions() {
			return internalElement.getTypeNameOrAliasOptions();
		}

		public String getDefaultValueExpression() {
			return defaultValueExpression;
		}

		public void setDefaultValueExpression(String defaultValueExpression) {
			this.defaultValueExpression = defaultValueExpression;
		}

		public boolean isVariant() {
			return variant;
		}

		public void setVariant(boolean variant) {
			this.variant = variant;
		}

		public boolean isNullable() {
			return nullable;
		}

		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			String builderClassName = operationClassName + "." + operationBuilderClassSimpleName;
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			String customizedTypeNameExpression = variant
					? (MiscUtils.adaptClassNameToSourceCode(VariantCustomizations.class.getName())
							+ ".getAdapterTypeName(" + builderClassName + ".class.getName(),\"" + builderElementName
							+ "\")")
					: (builderClassName + ".class.getName()");
			String customizedFieldNameExpression = variant
					? (MiscUtils.adaptClassNameToSourceCode(VariantCustomizations.class.getName())
							+ ".getConstantValueFieldName(\"" + builderElementName + "\")")
					: ("\"" + builderElementName + "\"");
			if (nullable) {
				result.append("" + InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
						+ customizedTypeNameExpression + ", " + customizedFieldNameExpression
						+ ").setNullValueDistinctForced(true);\n");
			}
		}

		@Override
		public Element getOperationClassElement(String operationClassName, String parameterName) {
			Structure.SimpleElement result = new Structure.SimpleElement();
			result.setName(parameterName);
			result.setTypeNameOrAlias(getTypeNameOrAlias());
			return result;
		}

		@Override
		protected Element getOperationBuilderClassElement(String operationClassName, String parameterName) {
			if (variant) {
				return new Structure.SimpleElement() {
					Element base = getOperationClassElement(operationClassName, parameterName);
					{
						setName(base.getName() + "Variant");
						String variantGenericParameterTypeName = adaptVariantGenericParameterTypeName(
								base.getTypeName(operationClassName));
						setTypeNameOrAlias(Variant.class.getName() + "<" + variantGenericParameterTypeName + ">");
						Structure.Optionality optionality = new Structure.Optionality();
						{
							optionality.setDefaultValueExpression("new " + getTypeNameOrAlias() + "("
									+ variantGenericParameterTypeName + ".class"
									+ ((defaultValueExpression != null) ? (", " + defaultValueExpression) : "") + ")");
							setOptionality(optionality);
						}
					}

					@Override
					protected String generateRequiredInnerJavaTypesSourceCode(String operationBuilderClassName,
							Map<Object, Object> options) {
						return base.generateRequiredInnerJavaTypesSourceCode(operationClassName, options);
					}
				};
			} else {
				Element result = getOperationClassElement(operationClassName, parameterName);
				if (defaultValueExpression != null) {
					Structure.Optionality optionality = new Structure.Optionality();
					optionality.setDefaultValueExpression(defaultValueExpression);
					result.setOptionality(optionality);
				}
				return result;
			}
		}

		protected String adaptVariantGenericParameterTypeName(String typeName) {
			Class<?> simpleClass = MiscUtils.getJESBClass(typeName);
			if (simpleClass.isPrimitive()) {
				simpleClass = ClassUtils.primitiveToWrapperClass(simpleClass);
			}
			return simpleClass.getName();
		}

		@Override
		protected String generateBuildExpression(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			Element operationBuilderElement = getOperationBuilderClassElement(operationClassName, parameterName);
			if (variant) {
				return "this." + operationBuilderElement.getName() + ".getValue()";
			} else {
				return "this." + operationBuilderElement.getName();
			}
		}

		@Override
		protected String generateBuilderRequiredInnerJavaTypesSourceCode(String operationClassName,
				String parameterName, Map<Object, Object> options) {
			return null;
		}

		public void validate() throws ValidationError {
			internalElement.validate(true);
		}
	}

	public static class ReferenceParameterNature extends ParameterNature {

		private String assetClassName;

		public String getAssetClassName() {
			return assetClassName;
		}

		public void setAssetClassName(String assetClassName) {
			this.assetClassName = assetClassName;
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			String builderClassName = operationClassName + "." + operationBuilderClassSimpleName;
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ builderClassName + ".class.getName(), \"" + builderElementName + "\")"
					+ ".setFormControlEmbeddingForced(true);\n");
		}

		public List<String> getAssetClassNameOptions() {
			List<String> result = new ArrayList<String>();
			result.addAll(JESBReflectionUI.getAllResourceMetadatas().stream()
					.map(metadata -> metadata.getResourceClass().getName()).collect(Collectors.toList()));
			result.add(Plan.class.getName());
			result.add(Folder.class.getName());
			result.add(JAR.class.getName());
			return result;
		}

		@Override
		public Element getOperationClassElement(String operationClassName, String parameterName) {
			Structure.SimpleElement result = new Structure.SimpleElement();
			result.setName(parameterName);
			result.setTypeNameOrAlias(assetClassName);
			return result;
		}

		@Override
		protected Element getOperationBuilderClassElement(String operationClassName, String parameterName) {
			Structure.SimpleElement result = new Structure.SimpleElement();
			result.setName(parameterName + "Reference");
			result.setTypeNameOrAlias(Reference.class.getName() + "<" + assetClassName + ">");
			Structure.Optionality optionality = new Structure.Optionality();
			{
				optionality.setDefaultValueExpression("new " + Reference.class.getName() + "<" + assetClassName + ">"
						+ "(" + assetClassName + ".class)");
				result.setOptionality(optionality);
			}
			return result;
		}

		@Override
		protected String generateBuildExpression(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			Element operationBuilderElement = getOperationBuilderClassElement(operationClassName, parameterName);
			return "this." + operationBuilderElement.getName() + ".resolve()";
		}

		@Override
		protected String generateBuilderRequiredInnerJavaTypesSourceCode(String operationClassName,
				String parameterName, Map<Object, Object> options) {
			return null;
		}

		public void validate() throws ValidationError {
			if (!getAssetClassNameOptions().contains(assetClassName)) {
				throw new ValidationError("Invalid referenced asset class name: '" + assetClassName + "'");
			}
		}

	}

	public static class EnumerationParameterNature extends ParameterNature {

		private Structure.EnumerationStructure structure = new Structure.EnumerationStructure();
		private String defaultValueExpression;
		private boolean variant = false;
		private boolean nullable = false;

		public Structure.EnumerationStructure getStructure() {
			return structure;
		}

		public void setStructure(Structure.EnumerationStructure structure) {
			this.structure = structure;
		}

		public String getDefaultValueExpression() {
			return defaultValueExpression;
		}

		public void setDefaultValueExpression(String defaultValueExpression) {
			this.defaultValueExpression = defaultValueExpression;
		}

		public boolean isVariant() {
			return variant;
		}

		public void setVariant(boolean variant) {
			this.variant = variant;
		}

		public boolean isNullable() {
			return nullable;
		}

		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			String builderClassName = operationClassName + "." + operationBuilderClassSimpleName;
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			String customizedTypeNameExpression = variant
					? (MiscUtils.adaptClassNameToSourceCode(VariantCustomizations.class.getName())
							+ ".getAdapterTypeName(" + builderClassName + ".class.getName(),\"" + builderElementName
							+ "\")")
					: (builderClassName + ".class.getName()");
			String customizedFieldNameExpression = variant
					? (MiscUtils.adaptClassNameToSourceCode(VariantCustomizations.class.getName())
							+ ".getConstantValueFieldName(\"" + builderElementName + "\")")
					: ("\"" + builderElementName + "\"");
			if (nullable) {
				result.append("" + InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
						+ customizedTypeNameExpression + ", " + customizedFieldNameExpression
						+ ").setNullValueDistinctForced(true);\n");
			}
		}

		private StructuredElement getEnumerationElementUtility(String operationClassName, String parameterName) {
			StructuredElement result = new StructuredElement();
			result.setName(parameterName);
			result.setStructure(structure);
			return result;
		}

		private SimpleParameterNature getSimpleParameterNatureUtility(String operationClassName, String parameterName) {
			SimpleParameterNature result = new SimpleParameterNature() {
				@Override
				protected String adaptVariantGenericParameterTypeName(String typeName) {
					return typeName;
				}
			};
			result.setDefaultValueExpression(defaultValueExpression);
			result.setVariant(variant);
			result.setTypeNameOrAlias(
					getEnumerationElementUtility(operationClassName, parameterName).getTypeName(operationClassName));
			return result;
		}

		public Element getOperationClassElement(String operationClassName, String parameterName) {
			return new ElementProxy(getSimpleParameterNatureUtility(operationClassName, parameterName)
					.getOperationClassElement(operationClassName, parameterName)) {
				StructuredElement enumerationElement = getEnumerationElementUtility(operationClassName, parameterName);

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
						Map<Object, Object> options) {
					return enumerationElement.generateRequiredInnerJavaTypesSourceCode(operationClassName, options);
				}
			};
		}

		@Override
		protected Element getOperationBuilderClassElement(String operationClassName, String parameterName) {
			return getSimpleParameterNatureUtility(operationClassName, parameterName)
					.getOperationBuilderClassElement(operationClassName, parameterName);
		}

		@Override
		protected String generateBuilderRequiredInnerJavaTypesSourceCode(String operationClassName,
				String parameterName, Map<Object, Object> options) {
			return getSimpleParameterNatureUtility(operationClassName, parameterName)
					.generateBuilderRequiredInnerJavaTypesSourceCode(operationClassName, parameterName, options);
		}

		@Override
		protected String generateBuildExpression(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			return getSimpleParameterNatureUtility(operationClassName, parameterName)
					.generateBuildExpression(operationClassName, parameterName, options);
		}

		public void validate() throws ValidationError {
			structure.validate(true);
			PluginBuilder.validateStructure(structure);
		}

	}

	public static class GroupParameterNature extends ParameterNature {

		private boolean nullable = false;

		private OperationDescriptor internalOperation = new OperationDescriptor() {

			@Override
			protected String getOperationBuilderClassSimpleName() {
				return "GroupBuilder";
			}

			@Override
			protected String generateExecutionMethodSourceCode(String className,
					Map<Object, Object> codeGenerationOptions) {
				return "";
			}

			@Override
			protected String generateOperationBuilderClassSourceCode(String operationClassName,
					Map<Object, Object> options) {
				return super.generateOperationBuilderClassSourceCode(operationClassName, options).replace(
						"implements " + OperationBuilder.class.getName(),
						"implements " + ParameterBuilder.class.getName());
			}

			@Override
			protected String generateOperationResultClassMethodSourceCode(String operationClassName,
					Map<Object, Object> options) {
				return "";
			}

		};

		public List<ParameterDescriptor> getParameters() {
			return internalOperation.getParameters();
		}

		public void setParameters(List<ParameterDescriptor> parameters) {
			internalOperation.setParameters(parameters);
		}

		public boolean isNullable() {
			return nullable;
		}

		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			String builderClassName = operationClassName + "." + operationBuilderClassSimpleName;
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			if (nullable) {
				result.append("" + InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
						+ builderClassName + ".class.getName()" + ", \"" + builderElementName
						+ "\").setNullValueDistinctForced(true);\n");
			}
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ builderClassName + ".class.getName(), \"" + builderElementName + "\")"
					+ ".setFormControlEmbeddingForced(true);\n");
			result.append(MiscUtils.adaptClassNameToSourceCode(
					getOperationBuilderClassElement(operationClassName, parameterName).getTypeName(operationClassName))
					+ "." + GUI.UI_CUSTOMIZATIONS_METHOD_NAME + "(" + uiCustomizationsVariableName + ")" + ";\n");
		}

		@Override
		public Element getOperationClassElement(String operationClassName, String parameterName) {
			Structure.StructuredElement result = new Structure.StructuredElement() {
				{
					setName(parameterName);
					setStructure(new Structure.ClassicStructure() {
						{
							String groupStructureTypeName = getTypeName(operationClassName);
							for (ParameterDescriptor parameter : getParameters()) {
								getElements().add(parameter.getOperationClassElement(groupStructureTypeName));
							}
						}

						@Override
						public String generateJavaTypeSourceCode(String groupStructureClassName,
								String additionalyImplemented, String additionalyExtended,
								String additionalDeclarations, Map<Object, Object> options) {
							internalOperation.setOpertionTypeName(groupStructureClassName);
							additionalDeclarations = ((additionalDeclarations != null) ? (additionalDeclarations + "\n")
									: "")
									+ internalOperation.generateOperationBuilderClassSourceCode(groupStructureClassName,
											options);
							return super.generateJavaTypeSourceCode(groupStructureClassName, additionalyImplemented,
									additionalyExtended, additionalDeclarations, options);
						}
					});
				}

			};
			return result;
		}

		@Override
		protected Element getOperationBuilderClassElement(String operationClassName, String parameterName) {
			return new Structure.StructuredElement() {
				{
					setName(parameterName + "GroupBuilder");
					setStructure(new Structure.ClassicStructure() {
						{
							String groupStructureTypeName = getOperationClassElement(operationClassName, parameterName)
									.getTypeName(operationClassName);
							for (ParameterDescriptor parameter : getParameters()) {
								getElements().add(parameter.getOperationBuilderClassElement(groupStructureTypeName));
							}
						}
					});
					Structure.Optionality optionality = new Structure.Optionality();
					{
						optionality.setDefaultValueExpression(
								"new " + getFinalTypeNameAdaptedToSourceCode(operationClassName) + "()");
						setOptionality(optionality);
					}

				}

				@Override
				protected String getTypeName(String parentClassName) {
					return getOperationClassElement(operationClassName, parameterName).getTypeName(operationClassName)
							+ "$" + internalOperation.getOperationBuilderClassSimpleName();
				}

			};
		}

		@Override
		protected String generateBuildExpression(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			Element operationBuilderElement = getOperationBuilderClassElement(operationClassName, parameterName);
			return "this." + operationBuilderElement.getName() + ".build(context, executionInspector)";
		}

		@Override
		protected String generateBuilderRequiredInnerJavaTypesSourceCode(String operationClassName,
				String parameterName, Map<Object, Object> options) {
			return null;
		}

		public void validate() throws ValidationError {
			for (ParameterDescriptor parameter : getParameters()) {
				parameter.validate();
			}

		}
	}

	public static class DynamicParameterNature extends ParameterNature {

		private Structure structure = new Structure.ClassicStructure();
		private List<StructureDerivationAlternative> concreteStructureAlternatives;

		public Structure getStructure() {
			return structure;
		}

		public void setStructure(Structure structure) {
			this.structure = structure;
		}

		public List<StructureDerivationAlternative> getConcreteStructureAlternatives() {
			return concreteStructureAlternatives;
		}

		public void setConcreteStructureAlternatives(
				List<StructureDerivationAlternative> concreteStructureAlternatives) {
			this.concreteStructureAlternatives = concreteStructureAlternatives;
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			String builderClassName = operationClassName + "." + operationBuilderClassSimpleName;
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ builderClassName + ".class.getName(), \"" + builderElementName + "\")"
					+ ".setFormControlEmbeddingForced(true);\n");
		}

		@Override
		public Structure.Element getOperationClassElement(String operationClassName, String parameterName) {
			Structure.StructuredElement result = new Structure.StructuredElement();
			result.setName(parameterName);
			result.setStructure(getStructure());
			return new Structure.ElementProxy(result) {
				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String operationClassName,
						Map<Object, Object> options) {
					options = new HashMap<Object, Object>(options);
					Structure.ElementAccessMode.PUBLIC_FIELD.set(options);
					if (concreteStructureAlternatives != null) {
						StringBuilder result = new StringBuilder();
						result.append("abstract "
								+ super.generateRequiredInnerJavaTypesSourceCode(operationClassName, options));
						for (StructureDerivationAlternative alternative : concreteStructureAlternatives) {
							result.append("\n" + alternative.generateRequiredInnerJavaTypesSourceCode(
									operationClassName, getBaseStructureTypeName(operationClassName, parameterName),
									structure, options));
						}
						return result.toString();
					} else {
						return super.generateRequiredInnerJavaTypesSourceCode(operationClassName, options);
					}
				}
			};
		}

		@Override
		protected Element getOperationBuilderClassElement(String operationClassName, String parameterName) {
			return new Structure.SimpleElement() {
				Element base = getOperationClassElement(operationClassName, parameterName);
				{
					setName(base.getName() + "DynamicBuilder");
					setTypeNameOrAlias(RootInstanceBuilder.class.getName());
					Structure.Optionality optionality = new Structure.Optionality();
					{
						String classNameArgumentExpression;
						if (concreteStructureAlternatives != null) {
							classNameArgumentExpression = "new " + getConcreteTypeNameAccessorClassName(parameterName)
									+ "()";
						} else {
							classNameArgumentExpression = base.getFinalTypeNameAdaptedToSourceCode(operationClassName)
									+ ".class.getName()";
						}
						optionality.setDefaultValueExpression("new " + RootInstanceBuilder.class.getName() + "(\""
								+ base.getName() + "Input\", " + classNameArgumentExpression + ")");
						setOptionality(optionality);
					}
				}

			};
		}

		private String getBaseStructureTypeName(String operationClassName, String parameterName) {
			return getOperationClassElement(operationClassName, parameterName).getTypeName(operationClassName);
		}

		private String getConcreteTypeNameAccessorClassName(String parameterName) {
			return parameterName.substring(0, 1).toUpperCase() + parameterName.substring(1) + "InputClassNameAccessor";
		}

		private String generateConcreteTypeNameAccessorClassSourceCode(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			result.append("private class " + getConcreteTypeNameAccessorClassName(parameterName) + " extends "
					+ Accessor.class.getName() + "<String> {\n");
			result.append("	@Override\n");
			result.append("	public String get() {\n");
			result.append(generateConcreteTypeNameGetterBody(operationClassName, parameterName, options) + "\n");
			result.append("	}\n");
			result.append("}");
			return result.toString();
		}

		protected String generateConcreteTypeNameGetterBody(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			for (StructureDerivationAlternative alternative : concreteStructureAlternatives) {
				result.append("		if (" + alternative.getCondition() + ") {\n");
				result.append("			return "
						+ alternative.getConcreteStructureTypeName(operationClassName,
								getBaseStructureTypeName(operationClassName, parameterName), structure)
						+ ".class.getName();\n");
				result.append("		}\n");
			}
			result.append("		return null;");
			return result.toString();
		}

		@Override
		protected String generateBuildExpression(String operationClassName, String parameterName,
				Map<Object, Object> options) {
			Element operationElement = getOperationClassElement(operationClassName, parameterName);
			Element operationBuilderElement = getOperationBuilderClassElement(operationClassName, parameterName);
			return "(" + operationElement.getFinalTypeNameAdaptedToSourceCode(operationClassName) + ") this."
					+ operationBuilderElement.getName() + ".build(new " + InstantiationContext.class.getName()
					+ "(context.getVariables(), context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()))";
		}

		@Override
		protected String generateBuilderRequiredInnerJavaTypesSourceCode(String operationClassName,
				String parameterName, Map<Object, Object> options) {
			if (concreteStructureAlternatives != null) {
				return generateConcreteTypeNameAccessorClassSourceCode(operationClassName, parameterName, options);
			}
			return null;
		}

		public void validate() throws ValidationError {
			structure.validate(true);
			PluginBuilder.validateStructure(structure);
		}
	}

	public static class StructureDerivationAlternative {

		private String alternativeName;
		private String condition;
		private Structure.ClassicStructure structureDerivation = new Structure.ClassicStructure();

		public String getAlternativeName() {
			return alternativeName;
		}

		public void setAlternativeName(String alternativeName) {
			this.alternativeName = alternativeName;
		}

		public Structure.ClassicStructure getStructureDerivation() {
			return structureDerivation;
		}

		public void setStructureDerivation(Structure.ClassicStructure structureDerivation) {
			this.structureDerivation = structureDerivation;
		}

		public String getCondition() {
			return condition;
		}

		public void setCondition(String condition) {
			this.condition = condition;
		}

		public String generateRequiredInnerJavaTypesSourceCode(String operationClassName, String baseStructureTypeName,
				Structure baseStructure, Map<Object, Object> options) {
			Structure.StructuredElement utilityElement = getConcreteStructureUtilityElement(baseStructureTypeName,
					baseStructure);
			return utilityElement.generateRequiredInnerJavaTypesSourceCode(operationClassName, options);
		}

		public String getConcreteStructureTypeName(String operationClassName, String baseStructureTypeName,
				Structure baseStructure) {
			Structure.StructuredElement utilityElement = getConcreteStructureUtilityElement(baseStructureTypeName,
					baseStructure);
			return utilityElement.getFinalTypeNameAdaptedToSourceCode(operationClassName);
		}

		private StructuredElement getConcreteStructureUtilityElement(String baseStructureTypeName,
				Structure baseStructure) {
			return new Structure.StructuredElement() {
				{
					setName(alternativeName + baseStructureTypeName.replaceAll("Structure$", ""));
					setStructure(new Structure.DerivedClassicStructure(baseStructureTypeName, baseStructure) {
						{
							getElements().addAll(structureDerivation.getElements());
						}
					});
				}

			};
		}

		public void validate() throws ValidationError {
			if ((alternativeName == null) || alternativeName.isEmpty()) {
				throw new ValidationError("Structure alternative name not provided");
			}
			if ((condition == null) || condition.isEmpty()) {
				throw new ValidationError("Structure alternative condition not provided");
			}
			structureDerivation.validate(true);
		}

	}

	public static class ResourceDescriptor {

		private String resourceTypeName;
		private String resourceTypeCaption;
		private byte[] resourceIconImageData;
		private List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
		private String validationMethodBody;

		public String getResourceTypeName() {
			return resourceTypeName;
		}

		public void setResourceTypeName(String resourceTypeName) {
			this.resourceTypeName = resourceTypeName;
		}

		public String getResourceTypeCaption() {
			return resourceTypeCaption;
		}

		public void setResourceTypeCaption(String resourceTypeCaption) {
			this.resourceTypeCaption = resourceTypeCaption;
		}

		public List<PropertyDescriptor> getProperties() {
			return properties;
		}

		public void setProperties(List<PropertyDescriptor> properties) {
			this.properties = properties;
		}

		public String getValidationMethodBody() {
			return validationMethodBody;
		}

		public void setValidationMethodBody(String validationMethodBody) {
			this.validationMethodBody = validationMethodBody;
		}

		public void loadIconImage(File file) throws IOException {
			resourceIconImageData = MiscUtils.readBinary(file);
		}

		public BufferedImage getIconImage() {
			if (resourceIconImageData == null) {
				return null;
			}
			try {
				return ImageIO.read(new ByteArrayInputStream(resourceIconImageData));
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
		}

		public File generateJavaSourceCode(File sourceDirectroy, String packageName) {
			String resourceClassName = packageName + "." + resourceTypeName;
			String extended = Resource.class.getName();
			Structure.ClassicStructure resourceStructure = new Structure.ClassicStructure();
			Map<Object, Object> codeGenerationOptions = Structure.ElementAccessMode.ACCESSORS.singleton();
			for (PropertyDescriptor property : properties) {
				resourceStructure.getElements()
						.add(property.getResourceClassElement(resourceClassName, property.getName()));
			}
			StringBuilder additionalDeclarations = new StringBuilder();
			{
				additionalDeclarations.append("@Override\n");
				additionalDeclarations.append("public void validate(boolean recursively) {\n");
				if (validationMethodBody != null) {
					additionalDeclarations.append(validationMethodBody + "\n");
				}
				additionalDeclarations.append("}\n");
			}
			additionalDeclarations
					.append(generateMetadataClassSourceCode(resourceClassName, codeGenerationOptions) + "\n");
			File javaFile = new File(sourceDirectroy, resourceClassName.replace(".", "/") + ".java");
			try {
				if (!javaFile.getParentFile().exists()) {
					MiscUtils.createDirectory(javaFile.getParentFile(), true);
				}
				MiscUtils.write(javaFile, resourceStructure.generateJavaTypeSourceCode(resourceClassName, null,
						extended, additionalDeclarations.toString(), codeGenerationOptions), false);
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			return javaFile;
		}

		private String generateMetadataClassSourceCode(String resourceClassName, Map<Object, Object> options) {
			String className = "Metadata";
			String resourceClassSimpleName = MiscUtils.extractSimpleNameFromClassName(resourceClassName);
			String implemented = ResourceMetadata.class.getName();
			StringBuilder result = new StringBuilder();
			result.append("public static class " + className + " implements " + implemented + "{" + "\n");
			{
				result.append("@Override\n");
				result.append("public String getResourceTypeName() {\n");
				result.append("return \"" + resourceTypeCaption + "\";" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public Class<? extends " + Resource.class.getName() + "> getResourceClass() {\n");
				result.append("return " + resourceClassSimpleName + ".class;" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public " + ResourcePath.class.getName() + " getResourceIconImagePath() {\n");
				result.append("return new " + ResourcePath.class.getName() + "(" + ResourcePath.class.getName()
						+ ".specifyClassPathResourceLocation(" + resourceClassSimpleName
						+ ".class.getName().replace(\".\", \"/\") + \".png\"));" + "\n");
				result.append("}\n");
			}
			result.append("}");
			return result.toString();
		}

		public void validate() throws ValidationError {
			if ((resourceTypeName == null) || resourceTypeName.isEmpty()) {
				throw new ValidationError("Resource class name not provided");
			}
			for (PropertyDescriptor property : properties) {
				property.validate();
			}
		}

		public Resource test() throws Exception {
			PluginBuilder.INSTANCE.prepareTesting();
			String fullClassName = PluginBuilder.INSTANCE.getPackageName() + "." + resourceTypeName;
			Resource resource = (Resource) MiscUtils.getJESBClass(fullClassName).newInstance();
			return resource;
		}
	}

	public static class PropertyDescriptor {

		private String name;
		private String caption;
		private PropertyNature nature = new SimplePropertyNature();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCaption() {
			return caption;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public PropertyNature getNature() {
			return nature;
		}

		public void setNature(PropertyNature nature) {
			this.nature = nature;
		}

		public Element getResourceClassElement(String resourceClassName, String propertyName) {
			return ensureNotReadOnly(nature.getResourceClassElement(resourceClassName, propertyName));
		}

		public void validate() throws ValidationError {
			if ((name == null) || name.isEmpty()) {
				throw new ValidationError("Property name not provided");
			}
			nature.validate();
		}

	}

	public abstract static class PropertyNature {

		protected abstract void validate() throws ValidationError;

		protected abstract Element getResourceClassElement(String resourceClassName, String propertyName);

	}

	public static class SimplePropertyNature extends PropertyNature {

		private SimpleParameterNature internalParameterNature = new SimpleParameterNature();

		public String getTypeNameOrAlias() {
			return internalParameterNature.getTypeNameOrAlias();
		}

		public void setTypeNameOrAlias(String typeNameOrAlias) {
			internalParameterNature.setTypeNameOrAlias(typeNameOrAlias);
		}

		public List<String> getTypeNameOrAliasOptions() {
			return internalParameterNature.getTypeNameOrAliasOptions();
		}

		public String getDefaultValueExpression() {
			return internalParameterNature.getDefaultValueExpression();
		}

		public void setDefaultValueExpression(String defaultValueExpression) {
			internalParameterNature.setDefaultValueExpression(defaultValueExpression);
		}

		public boolean isVariant() {
			return internalParameterNature.isVariant();
		}

		public void setVariant(boolean variant) {
			internalParameterNature.setVariant(variant);
		}

		public boolean isNullable() {
			return internalParameterNature.isNullable();
		}

		public void setNullable(boolean nullable) {
			internalParameterNature.setNullable(nullable);
		}

		@Override
		protected Element getResourceClassElement(String resourceClassName, String propertyName) {
			return internalParameterNature.getOperationBuilderClassElement(resourceClassName, propertyName);
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}

	}

	public static class EnumerationPropertyNature extends PropertyNature {

		private EnumerationParameterNature internalParameterNature = new EnumerationParameterNature();

		public EnumerationStructure getStructure() {
			return internalParameterNature.getStructure();
		}

		public void setStructure(EnumerationStructure structure) {
			internalParameterNature.setStructure(structure);
		}

		public String getDefaultValueExpression() {
			return internalParameterNature.getDefaultValueExpression();
		}

		public void setDefaultValueExpression(String defaultValueExpression) {
			internalParameterNature.setDefaultValueExpression(defaultValueExpression);
		}

		public boolean isVariant() {
			return internalParameterNature.isVariant();
		}

		public void setVariant(boolean variant) {
			internalParameterNature.setVariant(variant);
		}

		public boolean isNullable() {
			return internalParameterNature.isNullable();
		}

		public void setNullable(boolean nullable) {
			internalParameterNature.setNullable(nullable);
		}

		@Override
		protected Element getResourceClassElement(String resourceClassName, String propertyName) {
			return new ElementProxy(
					internalParameterNature.getOperationBuilderClassElement(resourceClassName, propertyName)) {
				Element operationElement = internalParameterNature.getOperationClassElement(resourceClassName,
						propertyName);

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
						Map<Object, Object> options) {
					return operationElement.generateRequiredInnerJavaTypesSourceCode(parentClassName, options);
				}

			};
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}

	}

	public static class ReferencePropertyNature extends PropertyNature {

		private ReferenceParameterNature internalParameterNature = new ReferenceParameterNature();

		public String getAssetClassName() {
			return internalParameterNature.getAssetClassName();
		}

		public void setAssetClassName(String assetClassName) {
			internalParameterNature.setAssetClassName(assetClassName);
		}

		public List<String> getAssetClassNameOptions() {
			return internalParameterNature.getAssetClassNameOptions();
		}

		@Override
		protected Element getResourceClassElement(String resourceClassName, String propertyName) {
			return new ElementProxy(
					internalParameterNature.getOperationBuilderClassElement(resourceClassName, propertyName)) {
				Element operationElement = internalParameterNature.getOperationClassElement(resourceClassName,
						propertyName);

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
						Map<Object, Object> options) {
					return operationElement.generateRequiredInnerJavaTypesSourceCode(parentClassName, options);
				}

			};
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}

	}

	public static class GroupPropertyNature extends PropertyNature {

		private GroupParameterNature internalParameterNature = new GroupParameterNature();

		public List<ParameterDescriptor> getParameters() {
			return internalParameterNature.getParameters();
		}

		public void setParameters(List<ParameterDescriptor> parameters) {
			internalParameterNature.setParameters(parameters);
		}

		public boolean isNullable() {
			return internalParameterNature.isNullable();
		}

		public void setNullable(boolean nullable) {
			internalParameterNature.setNullable(nullable);
		}

		@Override
		protected Element getResourceClassElement(String resourceClassName, String propertyName) {
			return new ElementProxy(
					internalParameterNature.getOperationBuilderClassElement(resourceClassName, propertyName)) {
				Element operationElement = internalParameterNature.getOperationClassElement(resourceClassName,
						propertyName);

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
						Map<Object, Object> options) {
					return operationElement.generateRequiredInnerJavaTypesSourceCode(resourceClassName, options);
				}

			};
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}
	}

	public static class ActivatorDescriptor {

		private String activatorTypeName;
		private String activatorTypeCaption;
		private byte[] activatorIconImageData;
		private List<AttributeDescriptor> attributes = new ArrayList<AttributeDescriptor>();
		private ClassOptionDescriptor inputClassOption;
		private ClassOptionDescriptor outputClassOption;
		private String validationMethodBody;
		private String handlerInitializationStatements;
		private String handlerFinalizationStatements;

		public String getActivatorTypeName() {
			return activatorTypeName;
		}

		public void setActivatorTypeName(String activatorTypeName) {
			this.activatorTypeName = activatorTypeName;
		}

		public String getActivatorTypeCaption() {
			return activatorTypeCaption;
		}

		public void setActivatorTypeCaption(String activatorTypeCaption) {
			this.activatorTypeCaption = activatorTypeCaption;
		}

		public String getHandlerInitializationStatements() {
			return handlerInitializationStatements;
		}

		public void setHandlerInitializationStatements(String handlerInitializationStatements) {
			this.handlerInitializationStatements = handlerInitializationStatements;
		}

		public String getHandlerFinalizationStatements() {
			return handlerFinalizationStatements;
		}

		public void setHandlerFinalizationStatements(String handlerFinalizationStatements) {
			this.handlerFinalizationStatements = handlerFinalizationStatements;
		}

		public List<AttributeDescriptor> getAttributes() {
			return attributes;
		}

		public void setAttributes(List<AttributeDescriptor> attributes) {
			this.attributes = attributes;
		}

		public ClassOptionDescriptor getInputClassOption() {
			return inputClassOption;
		}

		public void setInputClassOption(ClassOptionDescriptor inputClassOption) {
			this.inputClassOption = inputClassOption;
		}

		public ClassOptionDescriptor getOutputClassOption() {
			return outputClassOption;
		}

		public void setOutputClassOption(ClassOptionDescriptor outputClassOption) {
			this.outputClassOption = outputClassOption;
		}

		public String getValidationMethodBody() {
			return validationMethodBody;
		}

		public void setValidationMethodBody(String validationMethodBody) {
			this.validationMethodBody = validationMethodBody;
		}

		public void loadIconImage(File file) throws IOException {
			activatorIconImageData = MiscUtils.readBinary(file);
		}

		public BufferedImage getIconImage() {
			if (activatorIconImageData == null) {
				return null;
			}
			try {
				return ImageIO.read(new ByteArrayInputStream(activatorIconImageData));
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
		}

		public File generateJavaSourceCode(File sourceDirectroy, String packageName) {
			String activatorClassName = packageName + "." + activatorTypeName;
			String extended = Activator.class.getName();
			Structure.ClassicStructure activatorStructure = new Structure.ClassicStructure();
			Map<Object, Object> codeGenerationOptions = Structure.ElementAccessMode.ACCESSORS.singleton();
			for (AttributeDescriptor attribute : attributes) {
				activatorStructure.getElements()
						.add(attribute.getActivatorClassElement(activatorClassName, attribute.getName()));
			}
			StringBuilder additionalMethodDeclarations = new StringBuilder();
			StringBuilder additionalInnerClassesDeclarations = new StringBuilder();
			{
				additionalMethodDeclarations.append("@Override\n");
				additionalMethodDeclarations.append("public Class<?> getInputClass() {\n");
				if (inputClassOption == null) {
					additionalMethodDeclarations.append("return null;\n");
				} else {
					additionalInnerClassesDeclarations.append(inputClassOption
							.generateClassesSourceCode(activatorClassName, "inputClass", codeGenerationOptions) + "\n");
					additionalMethodDeclarations
							.append(inputClassOption.generateClassOptionMethodBody(activatorClassName, "inputClass",
									codeGenerationOptions) + "\n");
				}
				additionalMethodDeclarations.append("}\n");
			}
			{
				additionalMethodDeclarations.append("@Override\n");
				additionalMethodDeclarations.append("public Class<?> getOutputClass() {\n");
				if (outputClassOption == null) {
					additionalMethodDeclarations.append("return null;\n");
				} else {
					additionalInnerClassesDeclarations
							.append(outputClassOption.generateClassesSourceCode(activatorClassName, "outputClass",
									codeGenerationOptions) + "\n");
					additionalMethodDeclarations
							.append(outputClassOption.generateClassOptionMethodBody(activatorClassName, "outputClass",
									codeGenerationOptions) + "\n");
				}
				additionalMethodDeclarations.append("}\n");
			}
			{
				additionalMethodDeclarations.append("@Override\n");
				additionalMethodDeclarations.append("public boolean isAutomaticallyTriggerable() {\n");
				additionalMethodDeclarations.append("return true;\n");
				additionalMethodDeclarations.append("}");
			}
			{
				additionalMethodDeclarations.append("@Override\n");
				additionalMethodDeclarations.append("public void initializeAutomaticTrigger("
						+ ActivationHandler.class.getName() + " activationHandler) throws Exception {\n");
				if (handlerInitializationStatements != null) {
					additionalMethodDeclarations.append(handlerInitializationStatements + "\n");
				}
				additionalMethodDeclarations.append("}\n");
			}
			{
				additionalMethodDeclarations.append("@Override\n");
				additionalMethodDeclarations.append("public void finalizeAutomaticTrigger() throws Exception {\n");
				if (handlerFinalizationStatements != null) {
					additionalMethodDeclarations.append(handlerFinalizationStatements + "\n");
				}
				additionalMethodDeclarations.append("}\n");
			}
			{
				additionalMethodDeclarations.append("@Override\n");
				additionalMethodDeclarations.append("public boolean isAutomaticTriggerReady() {\n");
				additionalMethodDeclarations.append("return false;\n");
				additionalMethodDeclarations.append("}\n");
			}
			{
				additionalMethodDeclarations.append("@Override\n");
				additionalMethodDeclarations
						.append("public void validate(boolean recursively, " + Plan.class.getName() + " plan) {\n");
				if (validationMethodBody != null) {
					additionalMethodDeclarations.append(validationMethodBody + "\n");
				}
				additionalMethodDeclarations.append("}\n");
			}
			additionalInnerClassesDeclarations
					.append(generateMetadataClassSourceCode(activatorClassName, codeGenerationOptions) + "\n");
			File javaFile = new File(sourceDirectroy, activatorClassName.replace(".", "/") + ".java");
			try {
				if (!javaFile.getParentFile().exists()) {
					MiscUtils.createDirectory(javaFile.getParentFile(), true);
				}
				MiscUtils
						.write(javaFile,
								activatorStructure.generateJavaTypeSourceCode(activatorClassName, null, extended,
										additionalMethodDeclarations.toString() + "\n"
												+ additionalInnerClassesDeclarations.toString(),
										codeGenerationOptions),
								false);
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			return javaFile;
		}

		private String generateMetadataClassSourceCode(String resourceClassName, Map<Object, Object> options) {
			String className = "Metadata";
			String activatorClassSimpleName = MiscUtils.extractSimpleNameFromClassName(resourceClassName);
			String implemented = ActivatorMetadata.class.getName();
			StringBuilder result = new StringBuilder();
			result.append("public static class " + className + " implements " + implemented + "{" + "\n");
			{
				result.append("@Override\n");
				result.append("public String getActivatorName() {\n");
				result.append("return \"" + activatorTypeCaption + "\";" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public Class<? extends " + Activator.class.getName() + "> getActivatorClass() {\n");
				result.append("return " + activatorClassSimpleName + ".class;" + "\n");
				result.append("}\n");
			}
			{
				result.append("@Override\n");
				result.append("public " + ResourcePath.class.getName() + " getActivatorIconImagePath() {\n");
				result.append("return new " + ResourcePath.class.getName() + "(" + ResourcePath.class.getName()
						+ ".specifyClassPathResourceLocation(" + activatorClassSimpleName
						+ ".class.getName().replace(\".\", \"/\") + \".png\"));" + "\n");
				result.append("}\n");
			}
			result.append("}");
			return result.toString();
		}

		public void validate() throws ValidationError {
			if ((activatorTypeName == null) || activatorTypeName.isEmpty()) {
				throw new ValidationError("Activator class name not provided");
			}
			for (AttributeDescriptor attribute : attributes) {
				attribute.validate();
			}
		}

		public com.otk.jesb.activation.Experiment test() throws Exception {
			PluginBuilder.INSTANCE.prepareTesting();
			String fullClassName = PluginBuilder.INSTANCE.getPackageName() + "." + activatorTypeName;
			Activator activator = (Activator) MiscUtils.getJESBClass(fullClassName).newInstance();
			return new com.otk.jesb.activation.Experiment(activator);
		}
	}

	public static class AttributeDescriptor {

		private String name;
		private String caption;
		private AttributeNature nature = new SimpleAttributeNature();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCaption() {
			return caption;
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public AttributeNature getNature() {
			return nature;
		}

		public void setNature(AttributeNature nature) {
			this.nature = nature;
		}

		public Element getActivatorClassElement(String activatorClassName, String attributeName) {
			return ensureNotReadOnly(nature.getActivatorClassElement(activatorClassName, attributeName));
		}

		public void validate() throws ValidationError {
			if ((name == null) || name.isEmpty()) {
				throw new ValidationError("Attribute name not provided");
			}
			nature.validate();
		}

	}

	public abstract static class AttributeNature {

		protected abstract void validate() throws ValidationError;

		protected abstract Element getActivatorClassElement(String activatorClassName, String attributeName);
	}

	public static class SimpleAttributeNature extends AttributeNature {

		private SimpleParameterNature internalParameterNature = new SimpleParameterNature();

		public String getTypeNameOrAlias() {
			return internalParameterNature.getTypeNameOrAlias();
		}

		public void setTypeNameOrAlias(String typeNameOrAlias) {
			internalParameterNature.setTypeNameOrAlias(typeNameOrAlias);
		}

		public List<String> getTypeNameOrAliasOptions() {
			return internalParameterNature.getTypeNameOrAliasOptions();
		}

		public String getDefaultValueExpression() {
			return internalParameterNature.getDefaultValueExpression();
		}

		public void setDefaultValueExpression(String defaultValueExpression) {
			internalParameterNature.setDefaultValueExpression(defaultValueExpression);
		}

		public boolean isVariant() {
			return internalParameterNature.isVariant();
		}

		public void setVariant(boolean variant) {
			internalParameterNature.setVariant(variant);
		}

		public boolean isNullable() {
			return internalParameterNature.isNullable();
		}

		public void setNullable(boolean nullable) {
			internalParameterNature.setNullable(nullable);
		}

		@Override
		protected Element getActivatorClassElement(String activatorClassName, String attributeName) {
			return internalParameterNature.getOperationBuilderClassElement(activatorClassName, attributeName);
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}

	}

	public static class EnumerationAttributeNature extends AttributeNature {

		private EnumerationParameterNature internalParameterNature = new EnumerationParameterNature();

		public EnumerationStructure getStructure() {
			return internalParameterNature.getStructure();
		}

		public void setStructure(EnumerationStructure structure) {
			internalParameterNature.setStructure(structure);
		}

		public String getDefaultValueExpression() {
			return internalParameterNature.getDefaultValueExpression();
		}

		public void setDefaultValueExpression(String defaultValueExpression) {
			internalParameterNature.setDefaultValueExpression(defaultValueExpression);
		}

		public boolean isVariant() {
			return internalParameterNature.isVariant();
		}

		public void setVariant(boolean variant) {
			internalParameterNature.setVariant(variant);
		}

		public boolean isNullable() {
			return internalParameterNature.isNullable();
		}

		public void setNullable(boolean nullable) {
			internalParameterNature.setNullable(nullable);
		}

		@Override
		protected Element getActivatorClassElement(String activatorClassName, String attributeName) {
			return new ElementProxy(
					internalParameterNature.getOperationBuilderClassElement(activatorClassName, attributeName)) {
				Element operationElement = internalParameterNature.getOperationClassElement(activatorClassName,
						attributeName);

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
						Map<Object, Object> options) {
					return operationElement.generateRequiredInnerJavaTypesSourceCode(parentClassName, options);
				}

			};
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}

	}

	public static class ReferenceAttributeNature extends AttributeNature {

		private ReferenceParameterNature internalParameterNature = new ReferenceParameterNature();

		public String getAssetClassName() {
			return internalParameterNature.getAssetClassName();
		}

		public void setAssetClassName(String assetClassName) {
			internalParameterNature.setAssetClassName(assetClassName);
		}

		public List<String> getAssetClassNameOptions() {
			return internalParameterNature.getAssetClassNameOptions();
		}

		@Override
		protected Element getActivatorClassElement(String activatorClassName, String attributeName) {
			return new ElementProxy(
					internalParameterNature.getOperationBuilderClassElement(activatorClassName, attributeName)) {
				Element operationElement = internalParameterNature.getOperationClassElement(activatorClassName,
						attributeName);

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
						Map<Object, Object> options) {
					return operationElement.generateRequiredInnerJavaTypesSourceCode(parentClassName, options);
				}

			};
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}

	}

	public static class GroupAttributeNature extends AttributeNature {

		private GroupParameterNature internalParameterNature = new GroupParameterNature();

		public List<ParameterDescriptor> getParameters() {
			return internalParameterNature.getParameters();
		}

		public void setParameters(List<ParameterDescriptor> parameters) {
			internalParameterNature.setParameters(parameters);
		}

		public boolean isNullable() {
			return internalParameterNature.isNullable();
		}

		public void setNullable(boolean nullable) {
			internalParameterNature.setNullable(nullable);
		}

		@Override
		protected Element getActivatorClassElement(String activatorClassName, String attributeName) {
			return new ElementProxy(
					internalParameterNature.getOperationBuilderClassElement(activatorClassName, attributeName)) {
				Element operationElement = internalParameterNature.getOperationClassElement(activatorClassName,
						attributeName);

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String parentClassName,
						Map<Object, Object> options) {
					return operationElement.generateRequiredInnerJavaTypesSourceCode(activatorClassName, options);
				}

			};
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}
	}

}
