package com.otk.jesb.resource.builtin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.otk.jesb.JESB;
import com.otk.jesb.Log;
import com.otk.jesb.PotentialError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.Listener;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.Pair;
import com.otk.jesb.util.UpToDate;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

public abstract class XMLBasedDocumentResource extends WebDocumentBasedResource {

	protected abstract void runClassesGenerationTool(File mainFile, File metaSchemaFile, File outputDirectory)
			throws Exception;

	protected String text;
	protected Map<String, String> dependencyTextByFileName = new HashMap<String, String>();

	protected UpToDateGeneratedClasses upToDateGeneratedClasses = new UpToDateGeneratedClasses();

	public XMLBasedDocumentResource() {
		super();
	}

	public XMLBasedDocumentResource(String name) {
		super(name);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Map<String, String> getDependencyTextByFileName() {
		return dependencyTextByFileName;
	}

	public void setDependencyTextByFileName(Map<String, String> dependencyTextByFileName) {
		this.dependencyTextByFileName = dependencyTextByFileName;
	}

	public void load(Source source) {
		dependencyTextByFileName.clear();
		try {
			load(source, new Listener<String>() {
				@Override
				public void handle(String text) {
					setText(text);
				}
			});
		} catch (Exception e) {
			throw new PotentialError(e);
		}
	}

	protected String load(Source source, Listener<String> textHandler) throws Exception {
		try (InputStream in = source.getInputStream()) {
			String text = MiscUtils.read(in);
			for (String dependencyLocation : locateDependencies(text)) {
				String dependencyFileName = loadDependency(source, dependencyLocation);
				text = text.replace(dependencyLocation, dependencyFileName);
			}
			textHandler.handle(text);
		}
		return dependencyTextByFileName.size() + "_" + source.extractFileName();
	}

	protected List<String> locateDependencies(String text) {
		List<String> result = new ArrayList<String>();
		for (Pattern compiledPattern : new Pattern[] {
				Pattern.compile(
						"<(?:[a-zA-Z0-9_]+:)?import(?:\\s+namespace=\"[^\"]*\")?\\s+schemaLocation=\"([^\"]+)\"",
						Pattern.DOTALL),
				Pattern.compile("<(?:[a-zA-Z0-9_]+:)?include\\s+schemaLocation=\"([^\"]+)\"", Pattern.DOTALL),
				Pattern.compile("<\\s*!DOCTYPE[^>]+\"([^\"]+)\"\\s*>"),
				Pattern.compile("<\\s*!DOCTYPE[^>]+'([^']+)'\\s*>"),
				Pattern.compile("<\\s*!ENTITY[^>]+SYSTEM\\s+\"([^\"]+)\"\\s*>"),
				Pattern.compile("<\\s*!ENTITY[^>]+SYSTEM\\s+'([^']+)'\\s*>"),
				Pattern.compile("<\\s*!ENTITY[^>]+PUBLIC\\s+\"[^\"]+\"\\s+\"([^\"]+)\"\\s*>"),
				Pattern.compile("<\\s*!ENTITY[^>]+PUBLIC\\s+'[^']+'\\s+'([^']+)'\\s*>") }) {
			Matcher matcher = compiledPattern.matcher(text);
			while (matcher.find()) {
				result.add(matcher.group(1));
			}
		}
		return result;
	}

	protected String loadDependency(Source source, String dependencyLocation) throws Exception {
		URI sourceURI = source.toURI();
		URI dependencyURI = sourceURI.resolve(dependencyLocation);
		Source dependencySource;
		try {
			URL url = dependencyURI.toURL();
			dependencySource = new URLSource();
			((URLSource) dependencySource).setUrlSpecification(url.toString());
		} catch (MalformedURLException e) {
			File file = new File(dependencyURI);
			dependencySource = new FileSource();
			((FileSource) dependencySource).setFile(file);
		}
		final String[] dependencyTextHolder = new String[1];
		String dependencyFileName = load(dependencySource, new Listener<String>() {
			@Override
			public void handle(String text) {
				dependencyTextHolder[0] = text;
			}
		});
		dependencyTextByFileName.put(dependencyFileName, dependencyTextHolder[0]);
		return dependencyFileName;
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
		super.validate(recursively, solutionInstance);
		if ((text == null) || text.trim().isEmpty()) {
			throw new ValidationError("Text not provided");
		}
		try {
			upToDateGeneratedClasses.get(solutionInstance);
		} catch (Throwable t) {
			throw new ValidationError("Failed to validate the " + getClass().getSimpleName(), t);
		}
	}

	protected static class JAXBPostProcessor {

		private static final Pattern GETTER_PATTERN = Pattern.compile("^(?:get|is)(.*)");

		private static String getterToFieldName(String getterMethodName) {
			Matcher m = GETTER_PATTERN.matcher(getterMethodName);
			if (!m.matches()) {
				return null;
			}
			String result = m.group(1);
			if (result.length() > 0) {
				result = result.substring(0, 1).toLowerCase() + result.substring(1);
			}
			return result;
		}

		public static void process(File sourceDirectory) throws IOException {
			List<File> javaFiles = Files.walk(sourceDirectory.toPath()).filter(p -> p.toString().endsWith(".java"))
					.map(p -> p.toFile()).collect(Collectors.toList());
			for (File file : javaFiles) {
				CompilationUnit compilationUnit = StaticJavaParser.parse(file);
				compilationUnit.accept(new ModifierVisitor<Void>() {

					@Override
					public Visitable visit(FieldDeclaration fieldDeclaration, Void arg) {
						if (!isXMLPartFieldDeclaration(fieldDeclaration)) {
							return super.visit(fieldDeclaration, arg);
						}
						fieldDeclaration.getModifiers().clear();
						fieldDeclaration.addModifier(Modifier.Keyword.PUBLIC);
						if (isListFieldDeclaration(fieldDeclaration)) {
							fieldDeclaration.getVariables().forEach(variable -> {
								if (!variable.getInitializer().isPresent()) {
									variable.setInitializer(
											new ObjectCreationExpr().setType(ArrayList.class.getName() + "<>"));
								}
							});
						}
						return super.visit(fieldDeclaration, arg);
					}

					@Override
					public Visitable visit(MethodDeclaration methodDeclaration, Void arg) {
						if (!methodDeclaration.getModifiers().contains(new Modifier(Modifier.Keyword.PUBLIC))) {
							return super.visit(methodDeclaration, arg);
						}
						if (!methodDeclaration.getBody().isPresent()) {
							return super.visit(methodDeclaration, arg);
						}
						String methodName = methodDeclaration.getNameAsString();
						String getterFieldName = getterToFieldName(methodName);
						if (getterFieldName == null) {
							return super.visit(methodDeclaration, arg);
						}
						FieldDeclaration getterFieldDeclaration = compilationUnit.findFirst(FieldDeclaration.class,
								fieldDeclaration -> (getterFieldName
										.equals(fieldDeclaration.getVariable(0).getName().asString())
										|| ("_" + getterFieldName)
												.equals(fieldDeclaration.getVariable(0).getName().asString()))
										&& isXMLPartFieldDeclaration(fieldDeclaration))
								.orElse(null);
						if (getterFieldDeclaration == null) {
							return super.visit(methodDeclaration, arg);
						}
						return null;
					}

					private boolean isXMLPartFieldDeclaration(FieldDeclaration fieldDeclaration) {
						return fieldDeclaration.getAnnotations().stream()
								.anyMatch(annotation -> annotation.getNameAsString().startsWith("Xml"));
					}

					private boolean isListFieldDeclaration(FieldDeclaration fieldDeclaration) {
						return fieldDeclaration.getElementType().isClassOrInterfaceType() && fieldDeclaration
								.getElementType().asClassOrInterfaceType().getNameAsString().equals("List");
					}

				}, null);
				Files.write(file.toPath(), compilationUnit.toString().getBytes());
			}
		}
	}

	protected class UpToDateGeneratedClasses extends UpToDate<Solution, List<Class<?>>> {

		@Override
		protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
			return new Pair<String, Map<String, String>>(text, dependencyTextByFileName);
		}

		@Override
		protected List<Class<?>> obtainLatest(Solution solutionInstance, Object versionIdentifier)
				throws VersionAccessException {
			if (text == null) {
				return Collections.emptyList();
			}
			try {
				File directory = MiscUtils.createTemporaryDirectory();
				File mainFile = new File(directory,
						"main." + XMLBasedDocumentResource.this.getClass().getSimpleName().toLowerCase());
				File metaSchemaFile = new File(directory, "XMLSchema.xsd");
				File metaXMLFile = new File(directory, "xml.xsd");
				File metaSchemaDTDFile = new File(directory, "XMLSchema.dtd");
				File metaSchemaDatatypesDTDFile = new File(directory, "datatypes.dtd");
				Map<File, String> dependencyTextByFile = new HashMap<File, String>();
				for (Map.Entry<String, String> dependencyTextByFileNameEntry : dependencyTextByFileName.entrySet()) {
					dependencyTextByFile.put(new File(directory, dependencyTextByFileNameEntry.getKey()),
							dependencyTextByFileNameEntry.getValue());
				}
				try {
					MiscUtils.write(mainFile, text, false);
					MiscUtils.write(metaSchemaFile,
							MiscUtils.read(XSD.class.getResourceAsStream(metaSchemaFile.getName())), false);
					MiscUtils.write(metaXMLFile, MiscUtils.read(XSD.class.getResourceAsStream(metaXMLFile.getName())),
							false);
					MiscUtils.write(metaSchemaDTDFile,
							MiscUtils.read(XSD.class.getResourceAsStream(metaSchemaDTDFile.getName())), false);
					MiscUtils.write(metaSchemaDatatypesDTDFile,
							MiscUtils.read(XSD.class.getResourceAsStream(metaSchemaDatatypesDTDFile.getName())), false);
					for (Map.Entry<File, String> dependencyTextByFileEntry : dependencyTextByFile.entrySet()) {
						MiscUtils.write(dependencyTextByFileEntry.getKey(), dependencyTextByFileEntry.getValue(),
								false);
					}
					File sourceDirectory = MiscUtils.createTemporaryDirectory();
					try {
						runClassesGenerationTool(mainFile, metaSchemaFile, sourceDirectory);
						JAXBPostProcessor.process(sourceDirectory);
						return solutionInstance.getRuntime().getInMemoryCompiler().compile(sourceDirectory);
					} finally {
						MiscUtils.delete(sourceDirectory);
					}
				} finally {
					try {
						for (Map.Entry<File, String> dependencyTextByFileEntry : dependencyTextByFile.entrySet()) {
							MiscUtils.delete(dependencyTextByFileEntry.getKey());
						}
						MiscUtils.delete(metaSchemaDatatypesDTDFile);
						MiscUtils.delete(metaSchemaDTDFile);
						MiscUtils.delete(metaXMLFile);
						MiscUtils.delete(metaSchemaFile);
						MiscUtils.delete(mainFile);
						MiscUtils.delete(directory);
					} catch (Throwable ignore) {
						if (JESB.isDebugModeActive()) {
							Log.get().error(ignore);
						}
					}
				}
			} catch (Exception e) {
				throw new PotentialError(e);
			}
		}

	}
}