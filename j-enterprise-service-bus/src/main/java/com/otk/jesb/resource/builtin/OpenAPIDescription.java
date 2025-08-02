package com.otk.jesb.resource.builtin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.springframework.http.ResponseEntity;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.otk.jesb.JESB;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.instantiation.InstanceBuilder;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;
import xy.reflect.ui.info.type.source.JavaTypeInfoSource;
import xy.reflect.ui.util.ReflectionUIUtils;

import com.github.javaparser.ast.Node;

public class OpenAPIDescription extends WebDocumentBasedResource {

	private String text;

	private UpToDateGeneratedClasses upToDateGeneratedClasses = new UpToDateGeneratedClasses();
	private static WeakHashMap<Class<?>, Class<?>> serviceImplementationClassByInterface = new WeakHashMap<Class<?>, Class<?>>();

	public OpenAPIDescription() {
		this(OpenAPIDescription.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
	}

	public OpenAPIDescription(String name) {
		super(name);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void load(Source source) throws IOException {
		try (InputStream in = source.getInputStream()) {
			text = MiscUtils.read(in);
		}
	}

	protected void runClassesGenerationTool(File mainFile, File outputDirectory) throws Exception {
		{
			CodegenConfigurator configurator = new CodegenConfigurator();
			configurator.setInputSpec(mainFile.getPath());
			configurator.setGeneratorName("java");
			configurator.setOutputDir(outputDirectory.getPath());
			configurator.addAdditionalProperty("apiPackage", getClientPackageName());
			configurator.addAdditionalProperty("modelPackage", getClientPackageName());
			configurator.addAdditionalProperty("dateLibrary", "java8");
			configurator.addAdditionalProperty("library", "resttemplate");
			ClientOptInput clientOptInput = configurator.toClientOptInput();
			DefaultGenerator generator = new DefaultGenerator();
			generator.opts(clientOptInput).generate();
		}
		{
			CodegenConfigurator configurator = new CodegenConfigurator();
			configurator.setInputSpec(mainFile.getPath());
			configurator.setGeneratorName("jaxrs-cxf");
			configurator.setOutputDir(outputDirectory.getPath());
			configurator.addAdditionalProperty("apiPackage", getServicePackageName());
			configurator.addAdditionalProperty("modelPackage", getServicePackageName());
			configurator.addAdditionalProperty("dateLibrary", "java8");
			ClientOptInput clientOptInput = configurator.toClientOptInput();
			DefaultGenerator generator = new DefaultGenerator();
			generator.opts(clientOptInput).generate();
		}
	}

	private void generateSwaggerInitializeResourceClass(File sourceDirectory) throws IOException {
		StringBuilder swaggerInitializerResourceSourceCode = new StringBuilder();
		swaggerInitializerResourceSourceCode.append("package " + getServicePackageName() + ";\n");
		swaggerInitializerResourceSourceCode.append("\n");
		swaggerInitializerResourceSourceCode.append("import javax.ws.rs.GET;\n");
		swaggerInitializerResourceSourceCode.append("import javax.ws.rs.Path;\n");
		swaggerInitializerResourceSourceCode.append("import javax.ws.rs.Produces;\n");
		swaggerInitializerResourceSourceCode.append("import javax.ws.rs.core.Response;\n");
		swaggerInitializerResourceSourceCode.append("\n");
		swaggerInitializerResourceSourceCode.append("@Path(\"/swagger-initializer.js\")\n");
		swaggerInitializerResourceSourceCode.append("public class SwaggerInitializerResource {\n");
		swaggerInitializerResourceSourceCode.append("\n");
		swaggerInitializerResourceSourceCode.append("    @GET\n");
		swaggerInitializerResourceSourceCode.append("    @Produces(\"application/javascript\")\n");
		swaggerInitializerResourceSourceCode.append("    public Response get() {\n");
		swaggerInitializerResourceSourceCode.append("        String js = \" window.onload = function() {\\n\" + \n");
		swaggerInitializerResourceSourceCode
				.append("        		\"          window.ui = SwaggerUIBundle({\\n\" + \n");
		swaggerInitializerResourceSourceCode
				.append("        		\"            url: \\\"./openapi.json\\\",\\n\" + \n");
		swaggerInitializerResourceSourceCode.append("        		\"            dom_id: '#swagger-ui',\\n\" + \n");
		swaggerInitializerResourceSourceCode.append("        		\"            deepLinking: true,\\n\" + \n");
		swaggerInitializerResourceSourceCode.append("        		\"            presets: [\\n\" + \n");
		swaggerInitializerResourceSourceCode
				.append("        		\"              SwaggerUIBundle.presets.apis,\\n\" + \n");
		swaggerInitializerResourceSourceCode
				.append("        		\"              SwaggerUIStandalonePreset\\n\" + \n");
		swaggerInitializerResourceSourceCode.append("        		\"            ],\\n\" + \n");
		swaggerInitializerResourceSourceCode.append("        		\"            plugins: [\\n\" + \n");
		swaggerInitializerResourceSourceCode
				.append("        		\"              SwaggerUIBundle.plugins.DownloadUrl\\n\" + \n");
		swaggerInitializerResourceSourceCode.append("        		\"            ],\\n\" + \n");
		swaggerInitializerResourceSourceCode
				.append("        		\"            layout: \\\"StandaloneLayout\\\"\\n\" + \n");
		swaggerInitializerResourceSourceCode.append("        		\"          });\\n\" + \n");
		swaggerInitializerResourceSourceCode.append("        		\"        };\";\n");
		swaggerInitializerResourceSourceCode.append("        return Response.ok(js).build();\n");
		swaggerInitializerResourceSourceCode.append("    }\n");
		swaggerInitializerResourceSourceCode.append("}\n");
		swaggerInitializerResourceSourceCode.append("");
		File swaggerInitializerResourceSourceFile = new File(sourceDirectory,
				getServicePackageName().replace(".", "/") + "/SwaggerInitializerResource.java");
		Files.write(swaggerInitializerResourceSourceFile.toPath(),
				swaggerInitializerResourceSourceCode.toString().getBytes());
	}

	public Class<?> getAPIServiceInterface() {
		try {
			return upToDateGeneratedClasses
					.get().stream().filter(c -> c.getPackage().getName().equals(getServicePackageName())
							&& c.isInterface() && c.getAnnotation(io.swagger.annotations.Api.class) != null)
					.findFirst().orElse(null);
		} catch (VersionAccessException e) {
			throw new UnexpectedError(e);
		}
	}

	public Class<?> getSwaggerInitializerResourceClass() {
		try {
			return upToDateGeneratedClasses.get().stream()
					.filter(c -> c.getPackage().getName().equals(getServicePackageName())
							&& "SwaggerInitializerResource".equals(c.getSimpleName()))
					.findFirst().orElse(null);
		} catch (VersionAccessException e) {
			throw new UnexpectedError(e);
		}
	}

	public Class<?> getAPIServiceImplementationClass() {
		Class<?> currentServiceInterface = getAPIServiceInterface();
		if (currentServiceInterface == null) {
			return null;
		}
		synchronized (serviceImplementationClassByInterface) {
			serviceImplementationClassByInterface.computeIfAbsent(currentServiceInterface, serviceInterface -> {
				String className = serviceInterface.getName() + "Impl";
				StringBuilder javaSource = new StringBuilder();
				javaSource.append("package " + MiscUtils.extractPackageNameFromClassName(className) + ";" + "\n");
				javaSource.append("public class " + MiscUtils.extractSimpleNameFromClassName(className) + " implements "
						+ MiscUtils.adaptClassNameToSourceCode(serviceInterface.getName()) + "{" + "\n");
				javaSource.append("  private " + InvocationHandler.class.getName() + " invocationHandler;\n");
				javaSource.append("  public " + MiscUtils.extractSimpleNameFromClassName(className) + "("
						+ InvocationHandler.class.getName() + " invocationHandler){" + "\n");
				javaSource.append("    this.invocationHandler = invocationHandler;\n");
				javaSource.append("  }" + "\n");
				for (Method method : serviceInterface.getMethods()) {
					if (!Modifier.isAbstract(method.getModifiers())) {
						continue;
					}
					javaSource.append("  @Override\n");
					javaSource
							.append("  public " + MiscUtils.adaptClassNameToSourceCode(method.getReturnType().getName())
									+ " " + method.getName() + "("
									+ Arrays.stream(method.getParameters())
											.map(parameter -> MiscUtils.adaptClassNameToSourceCode(
													parameter.getType().getName()) + " " + parameter.getName())
											.collect(Collectors.joining(", "))
									+ ") {" + "\n");
					String methodReflectionAccessInstruction = MiscUtils
							.adaptClassNameToSourceCode(serviceInterface.getName()) + ".class.getMethod(\""
							+ method.getName() + "\", new Class[]{"
							+ Arrays.stream(method.getParameters())
									.map(parameter -> MiscUtils
											.adaptClassNameToSourceCode(parameter.getType().getName()) + ".class")
									.collect(Collectors.joining(", "))
							+ "})";
					String methodArgumentArrayCreationInstruction = "new Object[]{"
							+ Arrays.stream(method.getParameters()).map(parameter -> parameter.getName())
									.collect(Collectors.joining(", "))
							+ "}";
					javaSource.append("    try {\n");
					javaSource
							.append("        "
									+ (method.getReturnType().equals(void.class) ? ""
											: ("return (" + MiscUtils.adaptClassNameToSourceCode(
													method.getReturnType().getName()) + ")"))
									+ "invocationHandler.invoke(this, " + methodReflectionAccessInstruction + ", "
									+ methodArgumentArrayCreationInstruction + ");" + "\n");
					javaSource.append("    } catch (Throwable t) {\n");
					for (Class<?> exceptionType : method.getExceptionTypes()) {
						javaSource.append("			if(t instanceof "
								+ MiscUtils.adaptClassNameToSourceCode(exceptionType.getName()) + ") {\n");
						javaSource.append("				throw ("
								+ MiscUtils.adaptClassNameToSourceCode(exceptionType.getName()) + ")t;\n");
						javaSource.append("			}\n");
					}
					javaSource.append("			throw new "
							+ MiscUtils.adaptClassNameToSourceCode(UnexpectedError.class.getName()) + "(t);\n");
					javaSource.append("    }");
					javaSource.append("  }" + "\n");
				}
				javaSource.append("}" + "\n");
				try {
					return (Class<?>) MiscUtils.IN_MEMORY_COMPILER.compile(className, javaSource.toString());
				} catch (CompilationError e) {
					throw new UnexpectedError(e);
				}
			});
			return serviceImplementationClassByInterface.get(currentServiceInterface);
		}
	}

	public Class<?> getAPIClientClass() {
		try {
			return upToDateGeneratedClasses.get().stream().filter(
					c -> c.getPackage().getName().equals(getClientPackageName()) && Arrays.stream(c.getDeclaredFields())
							.anyMatch(field -> field.getType().equals(getAPIClientConfigurationClass())))
					.findFirst().orElse(null);
		} catch (VersionAccessException e) {
			throw new UnexpectedError(e);
		}
	}

	public Class<?> getAPIClientConfigurationClass() {
		try {
			return upToDateGeneratedClasses.get().stream().filter(
					c -> c.getPackage().getName().equals(getBasePackageName()) && c.getSimpleName().equals("ApiClient"))
					.findFirst().orElse(null);
		} catch (VersionAccessException e) {
			throw new UnexpectedError(e);
		}
	}

	public List<APIOperationDescriptor> getServiceOperationDescriptors() {
		Class<?> serviceInterface = getAPIServiceInterface();
		if (serviceInterface == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(serviceInterface.getDeclaredMethods()).stream().map(m -> new APIOperationDescriptor(m))
				.collect(Collectors.toList());
	}

	public List<APIOperationDescriptor> getClientOperationDescriptors() {
		Class<?> clientClass = getAPIClientClass();
		if (clientClass == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(clientClass.getDeclaredMethods()).stream()
				.filter(m -> (m.getReturnType() != ResponseEntity.class)
						&& (m.getReturnType() != getAPIClientConfigurationClass())
						&& !((m.getParameterCount() == 1)
								&& (m.getParameterTypes()[0] == getAPIClientConfigurationClass())))
				.map(m -> new APIOperationDescriptor(m)).collect(Collectors.toList());
	}

	public APIOperationDescriptor getServiceOperationDescriptor(String operationSignature) {
		return getServiceOperationDescriptors().stream()
				.filter(o -> o.getOperationSignature().equals(operationSignature)).findFirst().orElse(null);
	}

	public APIOperationDescriptor getClientOperationDescriptor(String operationSignature) {
		return getClientOperationDescriptors().stream()
				.filter(o -> o.getOperationSignature().equals(operationSignature)).findFirst().orElse(null);
	}

	private String getClientPackageName() {
		return getBasePackageName() + ".client";
	}

	private String getServicePackageName() {
		return getBasePackageName() + ".service";
	}

	private String getBasePackageName() {
		return InstantiationUtils.toRelativeTypeNameVariablePart(
				OpenAPIDescription.class.getName().toLowerCase() + MiscUtils.toDigitalUniqueIdentifier(this));
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		if ((text == null) || text.trim().isEmpty()) {
			throw new ValidationError("Text not provided");
		}
		try {
			upToDateGeneratedClasses.get();
		} catch (Throwable t) {
			throw new ValidationError("Failed to validate the " + getClass().getSimpleName(), t);
		}
	}

	protected class UpToDateGeneratedClasses extends UpToDate<List<Class<?>>> {

		@Override
		protected Object retrieveLastVersionIdentifier() {
			return text;
		}

		@Override
		protected List<Class<?>> obtainLatest(Object versionIdentifier) throws VersionAccessException {
			if (text == null) {
				return Collections.emptyList();
			}
			try {
				File directory = MiscUtils.createTemporaryDirectory();
				File mainFile = new File(directory, "main.oad");
				try {
					MiscUtils.write(mainFile, text, false);
					File sourceDirectory = MiscUtils.createTemporaryDirectory();
					try {
						runClassesGenerationTool(mainFile, sourceDirectory);
						generateSwaggerInitializeResourceClass(new File(sourceDirectory, "/src/gen/java"));
						CodeGenerationPostProcessor.process(sourceDirectory);
						List<Class<?>> result = new ArrayList<Class<?>>();
						result.addAll(MiscUtils.IN_MEMORY_COMPILER
								.compile(new File(sourceDirectory.getPath() + "/src/gen/java")));
						result.addAll(MiscUtils.IN_MEMORY_COMPILER
								.compile(new File(sourceDirectory.getPath() + "/src/main/java")));
						return result;
					} finally {
						MiscUtils.delete(sourceDirectory);
					}
				} finally {
					try {
						MiscUtils.delete(mainFile);
						MiscUtils.delete(directory);
					} catch (Throwable t) {
						if (JESB.DEBUG) {
							t.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				throw new UnexpectedError(e);
			}
		}

	}

	protected static class CodeGenerationPostProcessor {

		public static void process(File sourceDirectory) throws IOException {
			List<File> javaFiles = Files.walk(sourceDirectory.toPath()).filter(p -> p.toString().endsWith(".java"))
					.map(p -> p.toFile()).collect(Collectors.toList());
			for (File file : javaFiles) {
				CompilationUnit compilationUnit = StaticJavaParser.parse(file);
				compilationUnit.accept(new ModifierVisitor<Void>() {

					@Override
					public Node visit(ImportDeclaration importDeclaration, Void arg) {
						if (Arrays
								.asList("org.openapitools.jackson.nullable.JsonNullableModule",
										"io.swagger.jaxrs.PATCH", "org.apache.cxf.jaxrs.model.wadl.Description",
										"org.apache.cxf.jaxrs.model.wadl.DocTarget")
								.contains(importDeclaration.getNameAsString())) {
							return null;
						}
						return super.visit(importDeclaration, arg);
					}

				}, null);
				Files.write(file.toPath(), compilationUnit.toString().getBytes());
			}
		}
	}

	public static class APIOperationDescriptor {
		/*
		 * Cannot have simple inputClassByMethod/outputClassByMethod WeakHashMaps
		 * because the method object reference is not stable, unlike the method
		 * declaring class object reference that is then used as the WeakHashMap key.
		 */
		private static WeakHashMap<Class<?>, Map<Method, Class<? extends OperationInput>>> inputClassByMethodByDeclaringClass = new WeakHashMap<Class<?>, Map<Method, Class<? extends OperationInput>>>();
		private static WeakHashMap<Class<?>, Map<Method, Class<?>>> outputClassByMethodByDeclaringClass = new WeakHashMap<Class<?>, Map<Method, Class<?>>>();

		private Method operationMethod;

		public APIOperationDescriptor(Method m) {
			this.operationMethod = m;
		}

		public String getOperationSignature() {
			String result = ReflectionUIUtils.buildMethodSignature(operationMethod);
			result = InstantiationUtils.makeTypeNamesRelative(result, Arrays
					.asList(new InstanceBuilder(Accessor.returning(operationMethod.getDeclaringClass().getName()))));
			return result;
		}

		public Method retrieveMethod() {
			return operationMethod;
		}

		@SuppressWarnings("unchecked")
		public Class<? extends OperationInput> getOperationInputClass() {
			synchronized (inputClassByMethodByDeclaringClass) {
				inputClassByMethodByDeclaringClass.computeIfAbsent(operationMethod.getDeclaringClass(),
						(declaringClass -> new HashMap<Method, Class<? extends OperationInput>>()));
				inputClassByMethodByDeclaringClass.get(operationMethod.getDeclaringClass())
						.computeIfAbsent(operationMethod, operationMethod -> {
							String className = operationMethod.getDeclaringClass().getName() + "_"
									+ operationMethod.getName() + "." + OperationInput.class.getSimpleName();
							String additionalyImplemented = MiscUtils
									.adaptClassNameToSourceCode(OperationInput.class.getName());
							ClassicStructure stucture = new ClassicStructure();
							for (Parameter parameter : operationMethod.getParameters()) {
								SimpleElement element = new SimpleElement();
								element.setName(parameter.getName());
								String typeName = parameter.getType().getName();
								{
									List<Class<?>> genericTypeParameters = new JavaTypeInfoSource(parameter.getType(),
											operationMethod,
											Arrays.asList(operationMethod.getParameters()).indexOf(parameter), null)
													.guessGenericTypeParameters(parameter.getType());
									if (genericTypeParameters != null) {
										typeName += "<" + genericTypeParameters.stream().map(Class::getName)
												.collect(Collectors.joining(", ")) + ">";
									}
								}
								element.setTypeName(typeName);
								stucture.getElements().add(element);
							}
							StringBuilder additionalMethodDeclarations = new StringBuilder();
							additionalMethodDeclarations.append("  @Override" + "\n");
							additionalMethodDeclarations.append("  public Object[] listParameterValues() {" + "\n");
							additionalMethodDeclarations
									.append("  return new Object[] {"
											+ MiscUtils.stringJoin(Arrays.asList(operationMethod.getParameters())
													.stream().map(p -> p.getName()).collect(Collectors.toList()), ", ")
											+ "};" + "\n");
							additionalMethodDeclarations.append("  }" + "\n");
							try {
								return (Class<? extends OperationInput>) MiscUtils.IN_MEMORY_COMPILER.compile(className,
										stucture.generateJavaTypeSourceCode(className, additionalyImplemented, null,
												additionalMethodDeclarations.toString()));
							} catch (CompilationError e) {
								throw new UnexpectedError(e);
							}
						});
				return inputClassByMethodByDeclaringClass.get(operationMethod.getDeclaringClass()).get(operationMethod);
			}
		}

		@SuppressWarnings("unchecked")
		public Class<?> getOperationOutputClass() {
			synchronized (outputClassByMethodByDeclaringClass) {
				outputClassByMethodByDeclaringClass.computeIfAbsent(operationMethod.getDeclaringClass(),
						(declaringClass -> new HashMap<Method, Class<?>>()));
				outputClassByMethodByDeclaringClass.get(operationMethod.getDeclaringClass())
						.computeIfAbsent(operationMethod, operationMethod -> {
							if (operationMethod.getReturnType() == void.class) {
								return null;
							}
							String className = operationMethod.getDeclaringClass().getName() + "_"
									+ operationMethod.getName() + ".OperationOutput";
							ClassicStructure stucture = new ClassicStructure();
							{
								SimpleElement resultElement = new SimpleElement();
								resultElement.setName("result");
								String typeName = operationMethod.getReturnType().getName();
								{
									List<Class<?>> genericTypeParameters = new JavaTypeInfoSource(
											operationMethod.getReturnType(), operationMethod, -1, null)
													.guessGenericTypeParameters(operationMethod.getReturnType());
									if (genericTypeParameters != null) {
										typeName += "<" + genericTypeParameters.stream().map(Class::getName)
												.collect(Collectors.joining(", ")) + ">";
									}
								}
								resultElement.setTypeName(typeName);
								stucture.getElements().add(resultElement);
							}
							try {
								return (Class<? extends OperationInput>) MiscUtils.IN_MEMORY_COMPILER.compile(className,
										stucture.generateJavaTypeSourceCode(className));
							} catch (CompilationError e) {
								throw new UnexpectedError(e);
							}
						});
				return outputClassByMethodByDeclaringClass.get(operationMethod.getDeclaringClass())
						.get(operationMethod);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((operationMethod == null) ? 0 : operationMethod.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			APIOperationDescriptor other = (APIOperationDescriptor) obj;
			if (operationMethod == null) {
				if (other.operationMethod != null)
					return false;
			} else if (!operationMethod.equals(other.operationMethod))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Operation [signature=" + getOperationSignature() + "]";
		}

		public interface OperationInput {

			Object[] listParameterValues();

		}
	}

	public static class Metadata implements ResourceMetadata {

		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(OpenAPIDescription.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Resource> getResourceClass() {
			return OpenAPIDescription.class;
		}

		@Override
		public String getResourceTypeName() {
			return "OpenAPI Description";
		}

	}

}