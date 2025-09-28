package com.otk.jesb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
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
import com.otk.jesb.activation.ActivatorStructure;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.operation.OperationStructureBuilder;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.resource.ResourceStructure;
import com.otk.jesb.solution.Folder;
import com.otk.jesb.solution.JAR;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;
import com.otk.jesb.ui.GUI;
import com.otk.jesb.ui.GUI.VariantCustomizations;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.TreeVisitor;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import xy.reflect.ui.control.DefaultFieldControlInput;
import xy.reflect.ui.control.FieldControlDataProxy;
import xy.reflect.ui.control.FieldControlInputProxy;
import xy.reflect.ui.control.IFieldControlData;
import xy.reflect.ui.control.IFieldControlInput;
import xy.reflect.ui.control.plugin.AbstractSimpleCustomizableFieldControlPlugin.AbstractConfiguration;
import xy.reflect.ui.control.plugin.ICustomizableFieldControlPlugin;
import xy.reflect.ui.control.plugin.IFieldControlPlugin;
import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.custom.InfoCustomizations;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.enumeration.IEnumerationTypeInfo;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
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
		File metaInformationDirectroy = getMetaInformationDirectory(outputDirectory);
		MiscUtils.createDirectory(metaInformationDirectroy, true);
		generateMetaInformation(metaInformationDirectroy);
	}

	public void generateJAR(File jarFile) throws Exception {
		File temporaryDirectory = MiscUtils.createTemporaryDirectory();
		try {
			generateProject(temporaryDirectory);
			List<Class<?>> classes = new ArrayList<Class<?>>();
			classes.addAll(MiscUtils.IN_MEMORY_COMPILER.compile(getSourceDirectory(temporaryDirectory)));
			classes = MiscUtils.expandWithEnclosedClasses(classes);
			Manifest manifest = new Manifest();
			try (FileInputStream in = new FileInputStream(
					new File(getMetaInformationDirectory(temporaryDirectory), "MANIFEST.MF"))) {
				manifest.read(in);
			}
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

	private File getMetaInformationDirectory(File projectDirectory) {
		return new File(projectDirectory, "META-INF");
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

	private void generateMetaInformation(File metaInformationDirectory) {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(JAR.PLUGIN_OPERATION_METADATA_CLASSES_MANIFEST_KEY,
				operations.stream().map(operation -> packageName + "." + operation.getOpertionTypeName() + "$Metadata")
						.collect(Collectors.joining(",")));
		manifest.getMainAttributes().put(JAR.PLUGIN_ACTIVATOR_METADATA_CLASSES_MANIFEST_KEY,
				activators.stream().map(activator -> packageName + "." + activator.getActivatorTypeName() + "$Metadata")
						.collect(Collectors.joining(",")));
		manifest.getMainAttributes().put(JAR.PLUGIN_RESOURCE_METADATA_CLASSES_MANIFEST_KEY,
				resources.stream().map(resource -> packageName + "." + resource.getResourceTypeName() + "$Metadata")
						.collect(Collectors.joining(",")));
		try (FileOutputStream out = new FileOutputStream(new File(metaInformationDirectory, "MANIFEST.MF"))) {
			manifest.write(out);
		} catch (IOException e) {
			throw new UnexpectedError(e);
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
		try {
			unprepareTesting();
		} catch (Exception e) {
			throw new UnexpectedError(e);
		}
		try {
			File temporaryDirectory = MiscUtils.createTemporaryDirectory();
			try {
				generateSources(temporaryDirectory);
				MiscUtils.IN_MEMORY_COMPILER.compile(temporaryDirectory);
			} finally {
				MiscUtils.delete(temporaryDirectory);
			}
		} catch (IOException e) {
			throw new UnexpectedError(e);
		} catch (Throwable t) {
			throw new ValidationError(t.toString(), t);
		}
	}

	public static String writeControlPluginConfiguration(Object controlPluginConfiguration) {
		StringWriter writer = new StringWriter();
		MiscUtils.XSTREAM.marshal(controlPluginConfiguration, new CompactWriter(writer));
		return writer.toString();
	}

	public static Object readControlPluginConfiguration(String string) {
		return MiscUtils.XSTREAM.fromXML(string);
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

	private static Object getControlPluginConfigurationOnIdentifierUpdate(String controlPluginIdentifier,
			Object oldControlPluginConfiguration) {
		if (controlPluginIdentifier != null) {
			IFieldControlPlugin selectedControlPlugin = GUI.INSTANCE
					.obtainSubCustomizer(GUI.GLOBAL_EXCLUSIVE_CUSTOMIZATIONS).getFieldControlPlugins().stream()
					.filter(controlPlugin -> controlPlugin.getIdentifier().equals(controlPluginIdentifier)).findFirst()
					.get();
			if (selectedControlPlugin instanceof ICustomizableFieldControlPlugin) {
				Object defaultConfiguration = ((ICustomizableFieldControlPlugin) selectedControlPlugin)
						.getDefaultControlCustomization();
				if ((oldControlPluginConfiguration == null)
						|| (oldControlPluginConfiguration.getClass() != defaultConfiguration.getClass())) {
					return ((ICustomizableFieldControlPlugin) selectedControlPlugin).getDefaultControlCustomization();
				} else {
					return oldControlPluginConfiguration;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public static abstract class UIStructureBasedDescriptor {

		protected abstract List<? extends UIElementBasedDescriptor> getUIElements();

		protected abstract String getDisplayedTypeName(String prefix);

		protected abstract void generateUICustomizationStatements(StringBuilder result,
				UIElementBasedDescriptor uiElement, String uiCustomizationsVariableName, String displayedTypeName);

		private String additionalFieldDeclarationsSourceCode;
		private String additionalMethodDeclarationsSourceCode;
		private List<String> importedClassNames = new ArrayList<String>();

		public String getAdditionalFieldDeclarationsSourceCode() {
			return additionalFieldDeclarationsSourceCode;
		}

		public void setAdditionalFieldDeclarationsSourceCode(String additionalFieldDeclarationsSourceCode) {
			this.additionalFieldDeclarationsSourceCode = additionalFieldDeclarationsSourceCode;
		}

		public String getAdditionalMethodDeclarationsSourceCode() {
			return additionalMethodDeclarationsSourceCode;
		}

		public void setAdditionalMethodDeclarationsSourceCode(String additionalMethodDeclarationsSourceCode) {
			this.additionalMethodDeclarationsSourceCode = additionalMethodDeclarationsSourceCode;
		}

		public List<String> getImportedClassNames() {
			return importedClassNames;
		}

		public void setImportedClassNames(List<String> importedClassNames) {
			this.importedClassNames = importedClassNames;
		}

		protected String generateUICustomizationsMethodSourceCode(String displayedTypeNamePrefix) {
			StringBuilder result = new StringBuilder();
			String displayedTypeName = getDisplayedTypeName(displayedTypeNamePrefix);
			List<? extends UIElementBasedDescriptor> uiElements = getUIElements();
			result.append("public static void " + GUI.UI_CUSTOMIZATIONS_METHOD_NAME + "("
					+ InfoCustomizations.class.getName() + " infoCustomizations) {\n");
			result.append("/* " + displayedTypeName + " form customization */\n");
			{
				result.append("{\n");
				result.append("/* field control positions */\n");
				result.append(InfoCustomizations.class.getName() + ".getTypeCustomization(infoCustomizations, "
						+ displayedTypeName + ".class.getName()).setCustomFieldsOrder(" + Arrays.class.getName()
						+ ".asList("
						+ uiElements.stream().map(
								uiField -> '"' + uiField.getDisplayedFieldName(this, displayedTypeNamePrefix) + '"')
								.collect(Collectors.joining(", "))
						+ "));\n");
				for (UIElementBasedDescriptor uiElement : uiElements) {
					result.append("/* " + uiElement.getDisplayedFieldName(this, displayedTypeNamePrefix)
							+ " control customization */\n");
					result.append("{\n");
					if (uiElement.getCaption() != null) {
						result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
								+ displayedTypeName + ".class.getName(), \""
								+ uiElement.getDisplayedFieldName(this, displayedTypeNamePrefix)
								+ "\").setCustomFieldCaption(\"" + MiscUtils.escapeJavaString(uiElement.getCaption())
								+ "\");\n");
					}

					generateUICustomizationStatements(result, uiElement, "infoCustomizations", displayedTypeName);
					result.append("}\n");
				}
				result.append("/* hide UI customization method */\n");
				result.append(InfoCustomizations.class.getName() + ".getMethodCustomization(infoCustomizations, "
						+ displayedTypeName + ".class.getName(), " + ReflectionUIUtils.class.getName()
						+ ".buildMethodSignature(\"void\", \"" + GUI.UI_CUSTOMIZATIONS_METHOD_NAME + "\", "
						+ Arrays.class.getName() + ".asList(" + InfoCustomizations.class.getName()
						+ ".class.getName()))).setHidden(true);\n");
				result.append("}\n");
			}
			result.append("}");
			return result.toString();
		}

	}

	public static abstract class UIElementBasedDescriptor {

		private String name;
		private String caption;

		protected abstract String getDisplayedFieldName(UIStructureBasedDescriptor parent,
				String displayedTypeNamePrefix);

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

	}

	public static class OperationDescriptor extends UIStructureBasedDescriptor {

		private static final String RESULT_OPTION_NAME = "result";

		private String opertionTypeName;
		private String opertionTypeCaption;
		private String categoryName;
		private byte[] operationIconImageData;
		private List<ParameterDescriptor> parameters = new ArrayList<ParameterDescriptor>();
		private ClassOptionDescriptor result;
		private String executionMethodBody;
		private String additionalBuilderValidationStatements;

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

		public String getAdditionalBuilderValidationStatements() {
			return additionalBuilderValidationStatements;
		}

		public void setAdditionalBuilderValidationStatements(String additionalBuilderValidationStatements) {
			this.additionalBuilderValidationStatements = additionalBuilderValidationStatements;
		}

		public byte[] getOperationIconImageData() {
			return operationIconImageData;
		}

		public void setOperationIconImageData(byte[] operationIconImageData) {
			this.operationIconImageData = operationIconImageData;
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
			Map<Object, Object> codeGenerationOptions = Structure.ElementAccessMode.ACCESSORS.singletonOptions();
			for (ParameterDescriptor parameter : parameters) {
				operationStructure.getElements().add(parameter.getOperationClassElement(operationClassName));
			}
			StringBuilder afterPackageDeclaration = new StringBuilder();
			StringBuilder afterFieldDeclarations = new StringBuilder();
			StringBuilder afterMethodDeclarations = new StringBuilder();
			for (String importedClassName : getImportedClassNames()) {
				afterPackageDeclaration.append("import " + importedClassName + ";\n");
			}
			afterMethodDeclarations
					.append(generateExecutionMethodSourceCode(operationClassName, codeGenerationOptions) + "\n");
			afterMethodDeclarations
					.append(generateOperationBuilderClassSourceCode(operationClassName, codeGenerationOptions) + "\n");
			afterMethodDeclarations
					.append(generateMetadataClassSourceCode(operationClassName, codeGenerationOptions) + "\n");
			if (result != null) {
				afterMethodDeclarations.append(
						result.generateClassesSourceCode(operationClassName, RESULT_OPTION_NAME, codeGenerationOptions)
								+ "\n");
			}
			if (getAdditionalFieldDeclarationsSourceCode() != null) {
				afterFieldDeclarations.append(getAdditionalFieldDeclarationsSourceCode() + "\n");
			}
			if (getAdditionalMethodDeclarationsSourceCode() != null) {
				afterMethodDeclarations.append(getAdditionalMethodDeclarationsSourceCode() + "\n");
			}
			File javaFile = new File(sourceDirectroy, operationClassName.replace(".", "/") + ".java");
			try {
				if (!javaFile.getParentFile().exists()) {
					MiscUtils.createDirectory(javaFile.getParentFile(), true);
				}
				MiscUtils.write(javaFile,
						operationStructure.generateJavaTypeSourceCode(operationClassName, implemented, null,
								afterPackageDeclaration.toString(), afterFieldDeclarations.toString(),
								afterMethodDeclarations.toString(), codeGenerationOptions),
						false);
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
			StringBuilder afterMethodDeclarations = new StringBuilder();
			StringBuilder afterPackageDeclaration = new StringBuilder();
			for (String importedClassName : getImportedClassNames()) {
				afterPackageDeclaration.append("import " + importedClassName + ";\n");
			}
			afterMethodDeclarations.append(generateOperationBuildMethodSourceCode(operationClassName, options));
			afterMethodDeclarations.append(generateOperationResultClassMethodSourceCode(operationClassName, options));
			afterMethodDeclarations
					.append(generateOperationBuilderValidationMethodSourceCode(operationClassName, options));
			String packageName = MiscUtils.extractPackageNameFromClassName(operationClassName);
			afterMethodDeclarations.append(
					generateUICustomizationsMethodSourceCode((packageName != null) ? (packageName + ".") : "") + "\n");
			return "static " + operationBuilderStructure.generateJavaTypeSourceCode(
					getOperationBuilderClassSimpleName(), implemented, null, afterPackageDeclaration.toString(), null,
					afterMethodDeclarations.toString(), options);
		}

		@Override
		protected List<? extends UIElementBasedDescriptor> getUIElements() {
			return parameters;
		}

		@Override
		protected String getDisplayedTypeName(String prefix) {
			String operationClassName = prefix + opertionTypeName;
			String builderClassName = operationClassName + "." + getOperationBuilderClassSimpleName();
			return builderClassName;
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, UIElementBasedDescriptor uiElement,
				String uiCustomizationsVariableName, String displayedTypeName) {
			ParameterDescriptor parameter = (ParameterDescriptor) uiElement;
			String operationClassName = displayedTypeName.substring(0,
					displayedTypeName.length() - ("." + getOperationBuilderClassSimpleName()).length());
			parameter.getNature().generateUICustomizationStatements(result, uiCustomizationsVariableName,
					operationClassName, getOperationBuilderClassSimpleName(), parameter.getName());
		}

		protected String generateOperationBuilderValidationMethodSourceCode(String operationClassName,
				Map<Object, Object> options) {
			StringBuilder result = new StringBuilder();
			result.append("@Override\n");
			result.append("public void validate(boolean recursively, " + Plan.class.getName() + " currentPlan, "
					+ Step.class.getName() + " currentStep) throws " + ValidationError.class.getName() + "{\n");
			for (ParameterDescriptor parameter : parameters) {
				parameter.getNature().generateOperationBuilderValidationStatements(result, operationClassName,
						parameter.getName(), parameter.getCaption());
			}
			if (additionalBuilderValidationStatements != null) {
				result.append(additionalBuilderValidationStatements + "\n");
			}
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

	public static class ParameterDescriptor extends UIElementBasedDescriptor {

		private ParameterNature nature = new SimpleParameterNature();

		public ParameterNature getNature() {
			return nature;
		}

		@Override
		protected String getDisplayedFieldName(UIStructureBasedDescriptor parent, String displayedTypeNamePrefix) {
			String builderClassName = parent.getDisplayedTypeName(displayedTypeNamePrefix);
			String operationClassName = builderClassName.substring(0, builderClassName.length()
					- ("." + ((OperationDescriptor) parent).getOperationBuilderClassSimpleName()).length());
			return getOperationBuilderClassElement(operationClassName).getName();
		}

		public void setNature(ParameterNature nature) {
			this.nature = nature;
		}

		public Element getOperationClassElement(String operationClassName) {
			Element result = nature.getOperationClassElement(operationClassName, getName());
			return result;
		}

		public Element getOperationBuilderClassElement(String operationClassName) {
			Element result = nature.getOperationBuilderClassElement(operationClassName, getName());
			result = new Structure.ElementProxy(result) {

				@Override
				protected String generateRequiredInnerJavaTypesSourceCode(String operationBuilderClassName,
						Map<Object, Object> options) {
					return nature.generateBuilderRequiredInnerJavaTypesSourceCode(operationClassName,
							ParameterDescriptor.this.getName(), options);
				}

			};
			result = ensureNotReadOnly(result);
			return result;
		}

		public String generateBuildStatement(String targetVariableName, String operationClassName,
				Map<Object, Object> options) {
			return targetVariableName + " = " + nature.generateBuildExpression(operationClassName, getName(), options)
					+ ";";
		}

		public void validate() throws ValidationError {
			if ((getName() == null) || getName().isEmpty()) {
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

		protected abstract void generateOperationBuilderValidationStatements(StringBuilder result,
				String operationClassName, String parameterName, String parameterCaption);

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
		private String controlPluginIdentifier;
		private Object controlPluginConfiguration;

		public String getTypeNameOrAlias() {
			return internalElement.getTypeNameOrAlias();
		}

		public void setTypeNameOrAlias(String typeNameOrAlias) {
			internalElement.setTypeNameOrAlias(typeNameOrAlias);
		}

		public List<String> getTypeNameOrAliasOptions() {
			List<String> result = new ArrayList<String>(internalElement.getTypeNameOrAliasOptions());
			if (internalElement.getTypeNameOrAlias() != null) {
				if (!internalElement.getTypeNameOrAliasOptions().contains(internalElement.getTypeNameOrAlias())) {
					result.add(internalElement.getTypeNameOrAlias());
				}
			}
			return result;
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

		public String getControlPluginIdentifier() {
			return controlPluginIdentifier;
		}

		public void setControlPluginIdentifier(String controlPluginIdentifier) {
			this.controlPluginIdentifier = controlPluginIdentifier;
			this.controlPluginConfiguration = PluginBuilder.getControlPluginConfigurationOnIdentifierUpdate(
					controlPluginIdentifier, controlPluginConfiguration);
		}

		public Object getControlPluginConfiguration() {
			return controlPluginConfiguration;
		}

		public void setControlPluginConfiguration(Object controlPluginConfiguration) {
			this.controlPluginConfiguration = controlPluginConfiguration;
		}

		public List<String> getControlPluginIdentifierOptions() {
			String typeName = Structure.SimpleElement.TYPE_NAME_BY_ALIAS.getOrDefault(getTypeNameOrAlias(),
					getTypeNameOrAlias());
			final ITypeInfo typeInfo = GUI.INSTANCE.getReflectionUI()
					.getTypeInfo(new JavaTypeInfoSource(MiscUtils.getJESBClass(typeName), null));
			IFieldControlInput sampleControlInput = new FieldControlInputProxy(
					new DefaultFieldControlInput(GUI.INSTANCE.getReflectionUI()) {

						@Override
						public IFieldControlData getControlData() {
							return new FieldControlDataProxy(super.getControlData()) {

								@Override
								public ITypeInfo getType() {
									return typeInfo;
								}

							};
						}
					});
			return GUI.INSTANCE.obtainSubCustomizer(GUI.GLOBAL_EXCLUSIVE_CUSTOMIZATIONS).getFieldControlPlugins()
					.stream().filter(controlPlugin -> controlPlugin.handles(sampleControlInput))
					.map(IFieldControlPlugin::getIdentifier).collect(Collectors.toList());
		}

		@Override
		protected void generateOperationBuilderValidationStatements(StringBuilder result, String operationClassName,
				String parameterName, String parameterCaption) {
			if (variant) {
				String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName)
						.getName();
				result.append("if (recursively) {\n");
				result.append("try {\n");
				result.append(builderElementName + ".validate();\n");
				result.append("} catch (" + ValidationError.class.getName() + " e) {\n");
				result.append("throw new " + ValidationError.class.getName() + "(\"Failed to validate '"
						+ parameterCaption + "'\", e);\n");
				result.append("}\n");
				result.append("}\n");
			}
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			String builderClassName = operationClassName
					+ ((operationBuilderClassSimpleName != null) ? ("." + operationBuilderClassSimpleName) : "");
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			String builderElementTypeName = getOperationBuilderClassElement(operationClassName, parameterName)
					.getFinalTypeNameAdaptedToSourceCode(builderClassName);
			String customizedTypeNameExpression = variant
					? (MiscUtils.adaptClassNameToSourceCode(VariantCustomizations.class.getName())
							+ ".getAdapterTypeName(" + builderClassName + ".class.getName(),\"" + builderElementName
							+ "\")")
					: (builderClassName + ".class.getName()");
			String customizedFieldNameExpression = variant
					? (MiscUtils.adaptClassNameToSourceCode(VariantCustomizations.class.getName())
							+ ".getConstantValueFieldName(\"" + builderElementName + "\")")
					: ("\"" + builderElementName + "\"");
			String customizedFieldTypeNameExpression = variant
					? (MiscUtils.adaptClassNameToSourceCode(adaptVariantGenericParameterTypeName(
							getOperationClassElement(operationClassName, parameterName)
									.getTypeName(operationClassName)))
							+ ".class.getName()")
					: (builderElementTypeName + ".class.getName()");
			if (nullable) {
				result.append(InfoCustomizations.class.getName() + ".getFieldCustomization("
						+ uiCustomizationsVariableName + ", " + customizedTypeNameExpression + ", "
						+ customizedFieldNameExpression + ").setNullValueDistinctForced(true);\n");
			}
			if (controlPluginIdentifier != null) {
				result.append(InfoCustomizations.class.getName() + ".getTypeCustomization("
						+ InfoCustomizations.class.getName() + ".getFieldCustomization(" + uiCustomizationsVariableName
						+ ", " + customizedTypeNameExpression + ", " + customizedFieldNameExpression
						+ ").getSpecificTypeCustomizations(), " + customizedFieldTypeNameExpression
						+ ").setSpecificProperties(new " + HashMap.class.getName() + "<String, Object>() {\n");
				result.append("private static final long serialVersionUID = 1L;\n");
				result.append("{\n");
				result.append(ReflectionUIUtils.class.getName() + ".setFieldControlPluginIdentifier(this, \""
						+ controlPluginIdentifier + "\");\n");
				if (controlPluginConfiguration != null) {
					result.append(ReflectionUIUtils.class.getName() + ".setFieldControlPluginConfiguration(this, \""
							+ controlPluginIdentifier + "\", (" + Serializable.class.getName() + ") "
							+ PluginBuilder.class.getName() + ".readControlPluginConfiguration(\""
							+ MiscUtils.escapeJavaString(writeControlPluginConfiguration(controlPluginConfiguration))
							+ "\"));\n");
				}
				result.append("}\n");
				result.append("});\n");
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
			if (controlPluginIdentifier != null) {
				List<String> options = getControlPluginIdentifierOptions();
				if (!options.contains(controlPluginIdentifier)) {
					throw new ValidationError("Invalid control plugin identifier: '" + controlPluginIdentifier
							+ "'. Expected " + options);
				}
			}
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
		protected void generateOperationBuilderValidationStatements(StringBuilder result, String operationClassName,
				String parameterName, String parameterCaption) {
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			result.append("if (" + builderElementName + ".resolve() == null) {\n");
			result.append("throw new " + ValidationError.class.getName() + "(\"Failed to resolve the '"
					+ parameterCaption + "' reference\");\n");
			result.append("}\n");
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			String builderClassName = operationClassName
					+ ((operationBuilderClassSimpleName != null) ? ("." + operationBuilderClassSimpleName) : "");
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ builderClassName + ".class.getName(), \"" + builderElementName + "\")"
					+ ".setFormControlEmbeddingForced(true);\n");
		}

		public List<String> getAssetClassNameOptions() {
			List<String> result = new ArrayList<String>();
			result.addAll(MiscUtils.getAllResourceMetadatas().stream()
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
		private String controlPluginIdentifier;
		private Object controlPluginConfiguration;

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

		public String getControlPluginIdentifier() {
			return controlPluginIdentifier;
		}

		public void setControlPluginIdentifier(String controlPluginIdentifier) {
			this.controlPluginIdentifier = controlPluginIdentifier;
			this.controlPluginConfiguration = PluginBuilder.getControlPluginConfigurationOnIdentifierUpdate(
					controlPluginIdentifier, controlPluginConfiguration);
		}

		public Object getControlPluginConfiguration() {
			return controlPluginConfiguration;
		}

		public void setControlPluginConfiguration(Object controlPluginConfiguration) {
			this.controlPluginConfiguration = controlPluginConfiguration;
		}

		public List<String> getControlPluginIdentifierOptions() {
			IFieldControlInput sampleControlInput = new FieldControlInputProxy(
					new DefaultFieldControlInput(GUI.INSTANCE.getReflectionUI()) {

						@Override
						public IFieldControlData getControlData() {
							return new FieldControlDataProxy(super.getControlData()) {

								@Override
								public ITypeInfo getType() {
									return IEnumerationTypeInfo.NULL_ENUMERATION_TYPE_INFO;
								}

							};
						}
					});
			return GUI.INSTANCE.obtainSubCustomizer(GUI.GLOBAL_EXCLUSIVE_CUSTOMIZATIONS).getFieldControlPlugins()
					.stream().filter(controlPlugin -> controlPlugin.handles(sampleControlInput))
					.map(IFieldControlPlugin::getIdentifier).collect(Collectors.toList());
		}

		@Override
		protected void generateOperationBuilderValidationStatements(StringBuilder result, String operationClassName,
				String parameterName, String parameterCaption) {
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			getSimpleParameterNatureUtility(operationClassName, parameterName).generateUICustomizationStatements(result,
					uiCustomizationsVariableName, operationClassName, operationBuilderClassSimpleName, parameterName);
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
			result.setControlPluginIdentifier(controlPluginIdentifier);
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
						"implements " + OperationStructureBuilder.class.getName());
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
		protected void generateOperationBuilderValidationStatements(StringBuilder result, String operationClassName,
				String parameterName, String parameterCaption) {
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			result.append("if (recursively) {\n");
			result.append("try {\n");
			result.append(builderElementName + ".validate(recursively, plan, step);\n");
			result.append("} catch (" + ValidationError.class.getName() + " e) {\n");
			result.append("throw new " + ValidationError.class.getName() + "(\"Failed to validate '" + parameterCaption
					+ "'\", e);\n");
			result.append("}\n");
			result.append("}\n");
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			String builderClassName = operationClassName
					+ ((operationBuilderClassSimpleName != null) ? ("." + operationBuilderClassSimpleName) : "");
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ builderClassName + ".class.getName()" + ", \"" + builderElementName
					+ "\").setValueValidityDetectionForced(true);\n");
			if (nullable) {
				result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
						+ builderClassName + ".class.getName()" + ", \"" + builderElementName
						+ "\").setNullValueDistinctForced(true);\n");
			}
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ builderClassName + ".class.getName(), \"" + builderElementName + "\")"
					+ ".setFormControlEmbeddingForced(true);\n");
			result.append(getOperationBuilderClassElement(operationClassName, parameterName)
					.getFinalTypeNameAdaptedToSourceCode(operationClassName) + "." + GUI.UI_CUSTOMIZATIONS_METHOD_NAME
					+ "(" + uiCustomizationsVariableName + ")" + ";\n");
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
								String afterPackageDeclaration, String afterFieldDeclarations,
								String afterMethodDeclarations, Map<Object, Object> options) {
							internalOperation.setOpertionTypeName(groupStructureClassName);
							afterMethodDeclarations = ((afterMethodDeclarations != null)
									? (afterMethodDeclarations + "\n")
									: "")
									+ internalOperation.generateOperationBuilderClassSourceCode(groupStructureClassName,
											options);
							return super.generateJavaTypeSourceCode(groupStructureClassName, additionalyImplemented,
									additionalyExtended, afterPackageDeclaration, afterFieldDeclarations,
									afterMethodDeclarations, options);
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
		protected void generateOperationBuilderValidationStatements(StringBuilder result, String operationClassName,
				String parameterName, String parameterCaption) {
			String builderElementName = getOperationBuilderClassElement(operationClassName, parameterName).getName();
			result.append("if (recursively) {\n");
			result.append("try {\n");
			result.append(builderElementName
					+ ".getFacade().validate(recursively, plan.getValidationContext(step).getVariableDeclarations());\n");
			result.append("} catch (" + ValidationError.class.getName() + " e) {\n");
			result.append("throw new " + ValidationError.class.getName() + "(\"Failed to validate '" + parameterCaption
					+ "'\", e);\n");
			result.append("}\n");
			result.append("}\n");
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String operationClassName, String operationBuilderClassSimpleName, String parameterName) {
			String builderClassName = operationClassName
					+ ((operationBuilderClassSimpleName != null) ? ("." + operationBuilderClassSimpleName) : "");
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

	public static class ResourceDescriptor extends UIStructureBasedDescriptor {

		private String resourceTypeName;
		private String resourceTypeCaption;
		private byte[] resourceIconImageData;
		private List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
		private String additionalValidationStatements;

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

		public String getAdditionalValidationStatements() {
			return additionalValidationStatements;
		}

		public void setAdditionalValidationStatements(String additionalValidationStatements) {
			this.additionalValidationStatements = additionalValidationStatements;
		}

		public byte[] getResourceIconImageData() {
			return resourceIconImageData;
		}

		public void setResourceIconImageData(byte[] resourceIconImageData) {
			this.resourceIconImageData = resourceIconImageData;
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
			Map<Object, Object> codeGenerationOptions = Structure.ElementAccessMode.ACCESSORS.singletonOptions();
			String resourceClassName = packageName + "." + resourceTypeName;
			File javaFile = new File(sourceDirectroy, resourceClassName.replace(".", "/") + ".java");
			try {
				if (!javaFile.getParentFile().exists()) {
					MiscUtils.createDirectory(javaFile.getParentFile(), true);
				}
				MiscUtils.write(javaFile, generateJavaSourceCode(packageName, codeGenerationOptions), false);
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			return javaFile;
		}

		protected String generateJavaSourceCode(String packageName, Map<Object, Object> codeGenerationOptions) {
			String resourceClassName = ((packageName != null) ? (packageName + ".") : "") + resourceTypeName;
			String extended = Resource.class.getName();
			Structure.ClassicStructure resourceStructure = new Structure.ClassicStructure();
			for (PropertyDescriptor property : properties) {
				resourceStructure.getElements().add(property.getResourceClassElement(resourceClassName));
			}
			StringBuilder afterFieldDeclarations = new StringBuilder();
			StringBuilder afterMethodDeclarations = new StringBuilder();
			StringBuilder afterPackageDeclaration = new StringBuilder();
			for (String importedClassName : getImportedClassNames()) {
				afterPackageDeclaration.append("import " + importedClassName + ";\n");
			}
			afterMethodDeclarations.append(
					generateUICustomizationsMethodSourceCode((packageName != null) ? (packageName + ".") : "") + "\n");
			generateValidationMethodSourceCode(afterMethodDeclarations, resourceClassName, codeGenerationOptions);
			afterMethodDeclarations
					.append(generateMetadataClassSourceCode(resourceClassName, codeGenerationOptions) + "\n");
			if (getAdditionalFieldDeclarationsSourceCode() != null) {
				afterFieldDeclarations.append(getAdditionalFieldDeclarationsSourceCode() + "\n");
			}
			if (getAdditionalMethodDeclarationsSourceCode() != null) {
				afterMethodDeclarations.append(getAdditionalMethodDeclarationsSourceCode() + "\n");
			}
			return resourceStructure.generateJavaTypeSourceCode(resourceClassName, null, extended,
					afterPackageDeclaration.toString(), afterFieldDeclarations.toString(),
					afterMethodDeclarations.toString(), codeGenerationOptions);
		}

		protected void generateValidationMethodSourceCode(StringBuilder result, String resourceClassName,
				Map<Object, Object> codeGenerationOptions) {
			result.append("@Override\n");
			result.append(
					"public void validate(boolean recursively) throws " + ValidationError.class.getName() + " {\n");
			for (PropertyDescriptor property : properties) {
				property.getNature().generateResourceValidationStatements(result, resourceClassName, property.getName(),
						property.getCaption());
			}
			if (additionalValidationStatements != null) {
				result.append(additionalValidationStatements + "\n");
			}
			result.append("}\n");
		}

		protected String generateMetadataClassSourceCode(String resourceClassName, Map<Object, Object> options) {
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

		@Override
		protected List<? extends UIElementBasedDescriptor> getUIElements() {
			return properties;
		}

		@Override
		protected String getDisplayedTypeName(String prefix) {
			String resourceClassName = prefix + resourceTypeName;
			return resourceClassName;
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, UIElementBasedDescriptor uiElement,
				String uiCustomizationsVariableName, String displayedTypeName) {
			PropertyDescriptor property = (PropertyDescriptor) uiElement;
			String resourceClassName = displayedTypeName;
			property.getNature().generateUICustomizationStatements(result, uiCustomizationsVariableName,
					resourceClassName, property.getName());
		}

		public void validate() throws ValidationError {
			if ((resourceTypeName == null) || resourceTypeName.isEmpty()) {
				throw new ValidationError("Resource class name not provided");
			}
			for (PropertyDescriptor property : properties) {
				property.validate();
			}
		}

		public com.otk.jesb.resource.Experiment test() throws Exception {
			PluginBuilder.INSTANCE.prepareTesting();
			String fullClassName = PluginBuilder.INSTANCE.getPackageName() + "." + resourceTypeName;
			Resource resource = (Resource) MiscUtils.getJESBClass(fullClassName).newInstance();
			return new com.otk.jesb.resource.Experiment(resource);
		}
	}

	public static class PropertyDescriptor extends UIElementBasedDescriptor {

		private PropertyNature nature = new SimplePropertyNature();

		public PropertyNature getNature() {
			return nature;
		}

		public void setNature(PropertyNature nature) {
			this.nature = nature;
		}

		public Element getResourceClassElement(String resourceClassName) {
			return ensureNotReadOnly(nature.getResourceClassElement(resourceClassName, getName()));
		}

		@Override
		protected String getDisplayedFieldName(UIStructureBasedDescriptor parent, String displayedTypeNamePrefix) {
			String resourceClassName = parent.getDisplayedTypeName(displayedTypeNamePrefix);
			return getResourceClassElement(resourceClassName).getName();
		}

		public void validate() throws ValidationError {
			if ((getName() == null) || getName().isEmpty()) {
				throw new ValidationError("Property name not provided");
			}
			nature.validate();
		}

	}

	public abstract static class PropertyNature {

		protected abstract void validate() throws ValidationError;

		protected abstract void generateResourceValidationStatements(StringBuilder result, String resourceClassName,
				String propertyName, String caption);

		protected abstract void generateUICustomizationStatements(StringBuilder result,
				String uiCustomizationsVariableName, String resourceClassName, String name);

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

		public String getControlPluginIdentifier() {
			return internalParameterNature.getControlPluginIdentifier();
		}

		public void setControlPluginIdentifier(String controlPluginIdentifier) {
			internalParameterNature.setControlPluginIdentifier(controlPluginIdentifier);
		}

		public Object getControlPluginConfiguration() {
			return (AbstractConfiguration) internalParameterNature.getControlPluginConfiguration();
		}

		public void setControlPluginConfiguration(Object controlPluginConfiguration) {
			internalParameterNature.setControlPluginConfiguration(controlPluginConfiguration);
		}

		public List<String> getControlPluginIdentifierOptions() {
			return internalParameterNature.getControlPluginIdentifierOptions();
		}

		@Override
		protected Element getResourceClassElement(String resourceClassName, String propertyName) {
			return internalParameterNature.getOperationBuilderClassElement(resourceClassName, propertyName);
		}

		@Override
		protected void generateResourceValidationStatements(StringBuilder result, String resourceClassName,
				String propertyName, String propertyCaption) {
			internalParameterNature.generateOperationBuilderValidationStatements(result, resourceClassName,
					propertyName, propertyCaption);
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String resourceClassName, String propertyName) {
			internalParameterNature.generateUICustomizationStatements(result, uiCustomizationsVariableName,
					resourceClassName, null, propertyName);
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

		public String getControlPluginIdentifier() {
			return internalParameterNature.getControlPluginIdentifier();
		}

		public void setControlPluginIdentifier(String controlPluginIdentifier) {
			internalParameterNature.setControlPluginIdentifier(controlPluginIdentifier);
		}

		public Object getControlPluginConfiguration() {
			return internalParameterNature.getControlPluginConfiguration();
		}

		public void setControlPluginConfiguration(Object controlPluginConfiguration) {
			internalParameterNature.setControlPluginConfiguration(controlPluginConfiguration);
		}

		public List<String> getControlPluginIdentifierOptions() {
			return internalParameterNature.getControlPluginIdentifierOptions();
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
		protected void generateResourceValidationStatements(StringBuilder result, String resourceClassName,
				String propertyName, String propertyCaption) {
			internalParameterNature.generateOperationBuilderValidationStatements(result, resourceClassName,
					propertyName, propertyCaption);
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String resourceClassName, String propertyName) {
			internalParameterNature.generateUICustomizationStatements(result, uiCustomizationsVariableName,
					resourceClassName, null, propertyName);
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
		protected void generateResourceValidationStatements(StringBuilder result, String resourceClassName,
				String propertyName, String propertyCaption) {
			internalParameterNature.generateOperationBuilderValidationStatements(result, resourceClassName,
					propertyName, propertyCaption);
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String resourceClassName, String propertyName) {
			internalParameterNature.generateUICustomizationStatements(result, uiCustomizationsVariableName,
					resourceClassName, null, propertyName);
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}

	}

	public static class GroupPropertyNature extends PropertyNature {

		private boolean nullable = false;

		private ResourceDescriptor internalResource = new ResourceDescriptor() {

			@Override
			protected String generateMetadataClassSourceCode(String resourceClassName, Map<Object, Object> options) {
				return "";
			}

			@Override
			protected String generateJavaSourceCode(String packageName, Map<Object, Object> codeGenerationOptions) {
				return super.generateJavaSourceCode(packageName, codeGenerationOptions).replace(
						"extends " + Resource.class.getName(), "implements " + ResourceStructure.class.getName());
			}
		};

		public List<PropertyDescriptor> getProperties() {
			return internalResource.getProperties();
		}

		public void setProperties(List<PropertyDescriptor> properties) {
			internalResource.setProperties(properties);
		}

		public boolean isNullable() {
			return nullable;
		}

		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}

		@Override
		protected void generateResourceValidationStatements(StringBuilder result, String resourceClassName,
				String propertyName, String propertyCaption) {
			String elementName = getResourceClassElement(resourceClassName, propertyName).getName();
			result.append("if (recursively) {\n");
			result.append("try {\n");
			result.append(elementName + ".validate(recursively);\n");
			result.append("} catch (" + ValidationError.class.getName() + " e) {\n");
			result.append("throw new " + ValidationError.class.getName() + "(\"Failed to validate '" + propertyCaption
					+ "'\", e);\n");
			result.append("}\n");
			result.append("}\n");
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String resourceClassName, String propertyName) {
			String uiTypeName = resourceClassName;
			String uiElementName = getResourceClassElement(resourceClassName, propertyName).getName();
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ uiTypeName + ".class.getName()" + ", \"" + uiElementName
					+ "\").setValueValidityDetectionForced(true);\n");
			if (nullable) {
				result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
						+ uiTypeName + ".class.getName()" + ", \"" + uiElementName
						+ "\").setNullValueDistinctForced(true);\n");
			}
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ uiTypeName + ".class.getName(), \"" + uiElementName + "\")"
					+ ".setFormControlEmbeddingForced(true);\n");
			result.append(getResourceClassElement(resourceClassName, propertyName)
					.getFinalTypeNameAdaptedToSourceCode(resourceClassName) + "." + GUI.UI_CUSTOMIZATIONS_METHOD_NAME
					+ "(" + uiCustomizationsVariableName + ")" + ";\n");
		}

		@Override
		protected Element getResourceClassElement(String resourceClassName, String propertyName) {
			Structure.StructuredElement result = new Structure.StructuredElement() {
				{
					setName(propertyName);
					setStructure(new Structure.ClassicStructure() {
						{
							String groupStructureTypeName = getTypeName(resourceClassName);
							for (PropertyDescriptor property : getProperties()) {
								getElements().add(property.getResourceClassElement(groupStructureTypeName));
							}
						}

						@Override
						public String generateJavaTypeSourceCode(String groupStructureClassName,
								String additionalyImplemented, String additionalyExtended,
								String afterPackageDeclaration, String afterFieldDeclarations,
								String afterMethodDeclarations, Map<Object, Object> options) {
							internalResource.setResourceTypeName(groupStructureClassName);
							String packageName = MiscUtils.extractPackageNameFromClassName(groupStructureClassName);
							return internalResource.generateJavaSourceCode(packageName, options);
						}
					});
					Structure.Optionality optionality = new Structure.Optionality();
					{
						optionality.setDefaultValueExpression(
								"new " + getFinalTypeNameAdaptedToSourceCode(resourceClassName) + "()");
						setOptionality(optionality);
					}
				}
			};
			return result;
		}

		public void validate() throws ValidationError {
			for (PropertyDescriptor property : getProperties()) {
				property.validate();
			}
		}
	}

	public static class ActivatorDescriptor extends UIStructureBasedDescriptor {

		private String activatorTypeName;
		private String activatorTypeCaption;
		private byte[] activatorIconImageData;
		private List<AttributeDescriptor> attributes = new ArrayList<AttributeDescriptor>();
		private ClassOptionDescriptor inputClassOption;
		private ClassOptionDescriptor outputClassOption;
		private String additionalValidationStatements;
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

		public String getAdditionalValidationStatements() {
			return additionalValidationStatements;
		}

		public void setAdditionalValidationStatements(String additionalValidationStatements) {
			this.additionalValidationStatements = additionalValidationStatements;
		}

		public byte[] getActivatorIconImageData() {
			return activatorIconImageData;
		}

		public void setActivatorIconImageData(byte[] activatorIconImageData) {
			this.activatorIconImageData = activatorIconImageData;
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
			Map<Object, Object> codeGenerationOptions = Structure.ElementAccessMode.ACCESSORS.singletonOptions();
			String activatorClassName = packageName + "." + activatorTypeName;
			File javaFile = new File(sourceDirectroy, activatorClassName.replace(".", "/") + ".java");
			try {
				if (!javaFile.getParentFile().exists()) {
					MiscUtils.createDirectory(javaFile.getParentFile(), true);
				}
				MiscUtils.write(javaFile, generateJavaSourceCode(packageName, codeGenerationOptions), false);
			} catch (IOException e) {
				throw new UnexpectedError(e);
			}
			return javaFile;
		}

		protected String generateJavaSourceCode(String packageName, Map<Object, Object> codeGenerationOptions) {
			String activatorClassName = ((packageName != null) ? (packageName + ".") : "") + activatorTypeName;
			String extended = Activator.class.getName();
			Structure.ClassicStructure activatorStructure = new Structure.ClassicStructure();
			for (AttributeDescriptor attribute : attributes) {
				activatorStructure.getElements().add(attribute.getActivatorClassElement(activatorClassName));
			}
			StringBuilder afterPackageDeclaration = new StringBuilder();
			StringBuilder afterFieldDeclarations = new StringBuilder();
			StringBuilder afterMethodDeclarations = new StringBuilder();
			StringBuilder innerClassesDeclarations = new StringBuilder();
			for (String importedClassName : getImportedClassNames()) {
				afterPackageDeclaration.append("import " + importedClassName + ";\n");
			}
			afterFieldDeclarations.append(getActivationHandlerFieldDeclartionSourceCode() + "\n");
			generateInputSourceCode(activatorClassName, afterMethodDeclarations, innerClassesDeclarations,
					codeGenerationOptions);
			generateOutputSourceCode(activatorClassName, afterMethodDeclarations, innerClassesDeclarations,
					codeGenerationOptions);
			generateTriggerMethodsSourceCode(afterMethodDeclarations);
			afterMethodDeclarations.append(
					generateUICustomizationsMethodSourceCode((packageName != null) ? (packageName + ".") : "") + "\n");
			generateValidationMethodSourceCode(afterMethodDeclarations, activatorClassName);
			innerClassesDeclarations
					.append(generateMetadataClassSourceCode(activatorClassName, codeGenerationOptions) + "\n");
			if (getAdditionalFieldDeclarationsSourceCode() != null) {
				afterFieldDeclarations.append(getAdditionalFieldDeclarationsSourceCode() + "\n");
			}
			if (getAdditionalMethodDeclarationsSourceCode() != null) {
				afterMethodDeclarations.append(getAdditionalMethodDeclarationsSourceCode() + "\n");
			}
			return activatorStructure.generateJavaTypeSourceCode(activatorClassName, null, extended,
					afterPackageDeclaration.toString(), afterFieldDeclarations.toString(),
					afterMethodDeclarations.toString() + "\n" + innerClassesDeclarations.toString(),
					codeGenerationOptions);
		}

		protected String getActivationHandlerFieldDeclartionSourceCode() {
			return "private " + com.otk.jesb.activation.ActivationHandler.class.getName() + " activationHandler;";
		}

		protected void generateValidationMethodSourceCode(StringBuilder result, String activatorClassName) {
			result.append("@Override\n");
			result.append("public void validate(boolean recursively, " + Plan.class.getName() + " plan) throws "
					+ ValidationError.class.getName() + " {\n");
			result.append("super.validate(recursively, plan);\n");
			for (AttributeDescriptor attribute : attributes) {
				attribute.getNature().generateActivatorValidationStatements(result, activatorClassName,
						attribute.getName(), attribute.getCaption());
			}
			if (additionalValidationStatements != null) {
				result.append(additionalValidationStatements + "\n");
			}
			result.append("}\n");
		}

		protected void generateTriggerMethodsSourceCode(StringBuilder additionalMethodDeclarations) {
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
				additionalMethodDeclarations.append("this.activationHandler = activationHandler;\n");
				additionalMethodDeclarations.append("}\n");
			}
			{
				additionalMethodDeclarations.append("@Override\n");
				additionalMethodDeclarations.append("public void finalizeAutomaticTrigger() throws Exception {\n");
				additionalMethodDeclarations.append("this.activationHandler = null;\n");
				if (handlerFinalizationStatements != null) {
					additionalMethodDeclarations.append(handlerFinalizationStatements + "\n");
				}
				additionalMethodDeclarations.append("}\n");
			}
			{
				additionalMethodDeclarations.append("@Override\n");
				additionalMethodDeclarations.append("public boolean isAutomaticTriggerReady() {\n");
				additionalMethodDeclarations.append("return activationHandler != null;\n");
				additionalMethodDeclarations.append("}\n");
			}

		}

		protected void generateOutputSourceCode(String activatorClassName, StringBuilder additionalMethodDeclarations,
				StringBuilder additionalInnerClassesDeclarations, Map<Object, Object> codeGenerationOptions) {
			additionalMethodDeclarations.append("@Override\n");
			additionalMethodDeclarations.append("public Class<?> getOutputClass() {\n");
			if (outputClassOption == null) {
				additionalMethodDeclarations.append("return null;\n");
			} else {
				additionalInnerClassesDeclarations.append(outputClassOption
						.generateClassesSourceCode(activatorClassName, "outputClass", codeGenerationOptions) + "\n");
				additionalMethodDeclarations.append(outputClassOption.generateClassOptionMethodBody(activatorClassName,
						"outputClass", codeGenerationOptions) + "\n");
			}
			additionalMethodDeclarations.append("}\n");
		}

		protected void generateInputSourceCode(String activatorClassName, StringBuilder additionalMethodDeclarations,
				StringBuilder additionalInnerClassesDeclarations, Map<Object, Object> codeGenerationOptions) {
			additionalMethodDeclarations.append("@Override\n");
			additionalMethodDeclarations.append("public Class<?> getInputClass() {\n");
			if (inputClassOption == null) {
				additionalMethodDeclarations.append("return null;\n");
			} else {
				additionalInnerClassesDeclarations.append(inputClassOption.generateClassesSourceCode(activatorClassName,
						"inputClass", codeGenerationOptions) + "\n");
				additionalMethodDeclarations.append(inputClassOption.generateClassOptionMethodBody(activatorClassName,
						"inputClass", codeGenerationOptions) + "\n");
			}
			additionalMethodDeclarations.append("}\n");
		}

		protected String generateMetadataClassSourceCode(String resourceClassName, Map<Object, Object> options) {
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

		@Override
		protected List<? extends UIElementBasedDescriptor> getUIElements() {
			return attributes;
		}

		@Override
		protected String getDisplayedTypeName(String prefix) {
			String activatorClassName = prefix + activatorTypeName;
			return activatorClassName;
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, UIElementBasedDescriptor uiElement,
				String uiCustomizationsVariableName, String displayedTypeName) {
			AttributeDescriptor attribute = (AttributeDescriptor) uiElement;
			String activatorClassName = displayedTypeName;
			attribute.getNature().generateUICustomizationStatements(result, uiCustomizationsVariableName,
					activatorClassName, attribute.getName());
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

	public static class AttributeDescriptor extends UIElementBasedDescriptor {

		private AttributeNature nature = new SimpleAttributeNature();

		public AttributeNature getNature() {
			return nature;
		}

		public void setNature(AttributeNature nature) {
			this.nature = nature;
		}

		public Element getActivatorClassElement(String activatorClassName) {
			return ensureNotReadOnly(nature.getActivatorClassElement(activatorClassName, getName()));
		}

		@Override
		protected String getDisplayedFieldName(UIStructureBasedDescriptor parent, String displayedTypeNamePrefix) {
			String activatorClassName = parent.getDisplayedTypeName(displayedTypeNamePrefix);
			return getActivatorClassElement(activatorClassName).getName();
		}

		public void validate() throws ValidationError {
			if ((getName() == null) || getName().isEmpty()) {
				throw new ValidationError("Attribute name not provided");
			}
			nature.validate();
		}

	}

	public abstract static class AttributeNature {

		protected abstract void validate() throws ValidationError;

		protected abstract void generateActivatorValidationStatements(StringBuilder result, String activatorClassName,
				String attributeName, String attributeCaption);

		protected abstract void generateUICustomizationStatements(StringBuilder result,
				String uiCustomizationsVariableName, String activatorClassName, String attributeName);

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

		public String getControlPluginIdentifier() {
			return internalParameterNature.getControlPluginIdentifier();
		}

		public void setControlPluginIdentifier(String controlPluginIdentifier) {
			internalParameterNature.setControlPluginIdentifier(controlPluginIdentifier);
		}

		public Object getControlPluginConfiguration() {
			return internalParameterNature.getControlPluginConfiguration();
		}

		public void setControlPluginConfiguration(Object controlPluginConfiguration) {
			internalParameterNature.setControlPluginConfiguration(controlPluginConfiguration);
		}

		public List<String> getControlPluginIdentifierOptions() {
			return internalParameterNature.getControlPluginIdentifierOptions();
		}

		@Override
		protected void generateActivatorValidationStatements(StringBuilder result, String activatorClassName,
				String attributeName, String attributeCaption) {
			internalParameterNature.generateOperationBuilderValidationStatements(result, activatorClassName,
					attributeName, attributeCaption);
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String activatorClassName, String attributeName) {
			internalParameterNature.generateUICustomizationStatements(result, uiCustomizationsVariableName,
					activatorClassName, null, attributeName);
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

		public String getControlPluginIdentifier() {
			return internalParameterNature.getControlPluginIdentifier();
		}

		public void setControlPluginIdentifier(String controlPluginIdentifier) {
			internalParameterNature.setControlPluginIdentifier(controlPluginIdentifier);
		}

		public Object getControlPluginConfiguration() {
			return internalParameterNature.getControlPluginConfiguration();
		}

		public void setControlPluginConfiguration(Object controlPluginConfiguration) {
			internalParameterNature.setControlPluginConfiguration(controlPluginConfiguration);
		}

		public List<String> getControlPluginIdentifierOptions() {
			return internalParameterNature.getControlPluginIdentifierOptions();
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
		protected void generateActivatorValidationStatements(StringBuilder result, String activatorClassName,
				String attributeName, String attributeCaption) {
			internalParameterNature.generateOperationBuilderValidationStatements(result, activatorClassName,
					attributeName, attributeCaption);
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String activatorClassName, String attributeName) {
			internalParameterNature.generateUICustomizationStatements(result, uiCustomizationsVariableName,
					activatorClassName, null, attributeName);
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
		protected void generateActivatorValidationStatements(StringBuilder result, String activatorClassName,
				String attributeName, String attributeCaption) {
			internalParameterNature.generateOperationBuilderValidationStatements(result, activatorClassName,
					attributeName, attributeCaption);
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String activatorClassName, String attributeName) {
			internalParameterNature.generateUICustomizationStatements(result, uiCustomizationsVariableName,
					activatorClassName, null, attributeName);
		}

		@Override
		protected void validate() throws ValidationError {
			internalParameterNature.validate();
		}

	}

	public static class GroupAttributeNature extends AttributeNature {

		private boolean nullable = false;

		private ActivatorDescriptor internalActivator = new ActivatorDescriptor() {

			@Override
			protected void generateTriggerMethodsSourceCode(StringBuilder additionalMethodDeclarations) {
			}

			@Override
			protected void generateOutputSourceCode(String activatorClassName,
					StringBuilder additionalMethodDeclarations, StringBuilder additionalInnerClassesDeclarations,
					Map<Object, Object> codeGenerationOptions) {
			}

			@Override
			protected void generateInputSourceCode(String activatorClassName,
					StringBuilder additionalMethodDeclarations, StringBuilder additionalInnerClassesDeclarations,
					Map<Object, Object> codeGenerationOptions) {
			}

			@Override
			protected String generateMetadataClassSourceCode(String resourceClassName, Map<Object, Object> options) {
				return "";
			}

			@Override
			protected String getActivationHandlerFieldDeclartionSourceCode() {
				return "";
			}

			@Override
			protected void generateValidationMethodSourceCode(StringBuilder result, String activatorClassName) {
				StringBuilder tmp = new StringBuilder();
				super.generateValidationMethodSourceCode(tmp, activatorClassName);
				result.append(tmp.toString().replace("super.validate(recursively, plan);\n", ""));
			}

			@Override
			protected String generateJavaSourceCode(String packageName, Map<Object, Object> codeGenerationOptions) {
				return super.generateJavaSourceCode(packageName, codeGenerationOptions).replace(
						"extends " + Activator.class.getName(), "implements " + ActivatorStructure.class.getName());
			}

		};

		public List<AttributeDescriptor> getAttributes() {
			return internalActivator.getAttributes();
		}

		public void setAttributes(List<AttributeDescriptor> attributes) {
			internalActivator.setAttributes(attributes);
		}

		public boolean isNullable() {
			return nullable;
		}

		public void setNullable(boolean nullable) {
			this.nullable = nullable;
		}

		@Override
		protected void generateActivatorValidationStatements(StringBuilder result, String activatorClassName,
				String attributeName, String attributeCaption) {
			String elementName = getActivatorClassElement(activatorClassName, attributeName).getName();
			result.append("if (recursively) {\n");
			result.append("try {\n");
			result.append(elementName + ".validate(recursively, plan);\n");
			result.append("} catch (" + ValidationError.class.getName() + " e) {\n");
			result.append("throw new " + ValidationError.class.getName() + "(\"Failed to validate '" + attributeCaption
					+ "'\", e);\n");
			result.append("}\n");
			result.append("}\n");
		}

		@Override
		protected void generateUICustomizationStatements(StringBuilder result, String uiCustomizationsVariableName,
				String activatorClassName, String attributeName) {
			String uiTypeName = activatorClassName;
			String uiElementName = getActivatorClassElement(activatorClassName, attributeName).getName();
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ uiTypeName + ".class.getName()" + ", \"" + uiElementName
					+ "\").setValueValidityDetectionForced(true);\n");
			if (nullable) {
				result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
						+ uiTypeName + ".class.getName()" + ", \"" + uiElementName
						+ "\").setNullValueDistinctForced(true);\n");
			}
			result.append(InfoCustomizations.class.getName() + ".getFieldCustomization(infoCustomizations, "
					+ uiTypeName + ".class.getName(), \"" + uiElementName + "\")"
					+ ".setFormControlEmbeddingForced(true);\n");
			result.append(getActivatorClassElement(activatorClassName, attributeName)
					.getFinalTypeNameAdaptedToSourceCode(activatorClassName) + "." + GUI.UI_CUSTOMIZATIONS_METHOD_NAME
					+ "(" + uiCustomizationsVariableName + ")" + ";\n");
		}

		@Override
		protected Element getActivatorClassElement(String activatorClassName, String attributeName) {
			Structure.StructuredElement result = new Structure.StructuredElement() {
				{
					setName(attributeName);
					setStructure(new Structure.ClassicStructure() {
						{
							String groupStructureTypeName = getTypeName(activatorClassName);
							for (AttributeDescriptor attribute : getAttributes()) {
								getElements().add(attribute.getActivatorClassElement(groupStructureTypeName));
							}
						}

						@Override
						public String generateJavaTypeSourceCode(String groupStructureClassName,
								String additionalyImplemented, String additionalyExtended,
								String afterPackageDeclartion, String afterFieldDeclarations,
								String afterMethodDeclarations, Map<Object, Object> options) {
							internalActivator.setActivatorTypeName(groupStructureClassName);
							String packageName = MiscUtils.extractPackageNameFromClassName(groupStructureClassName);
							return internalActivator.generateJavaSourceCode(packageName, options);
						}
					});
					Structure.Optionality optionality = new Structure.Optionality();
					{
						optionality.setDefaultValueExpression(
								"new " + getFinalTypeNameAdaptedToSourceCode(activatorClassName) + "()");
						setOptionality(optionality);
					}
				}
			};
			return result;
		}

		public void validate() throws ValidationError {
			for (AttributeDescriptor attribute : getAttributes()) {
				attribute.validate();
			}
		}
	}

}
