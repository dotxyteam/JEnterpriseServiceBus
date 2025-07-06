package com.otk.jesb.resource.builtin;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.otk.jesb.JESB;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.util.Listener;
import com.otk.jesb.util.MiscUtils;

public abstract class XMLBasedDocumentResource extends WebDocumentBasedResource {

	protected abstract void runClassesGenerationTool(File mainFile, File metaSchemaFile, File outputDirectory)
			throws Exception;

	protected String text;
	protected Map<String, String> dependencyTextByFileName = new HashMap<String, String>();
	protected List<Class<?>> generatedClasses;

	public XMLBasedDocumentResource(String name) {
		super(name);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		generatedClasses = null;
	}

	public Map<String, String> getDependencyTextByFileName() {
		return Collections.unmodifiableMap(dependencyTextByFileName);
	}

	public void setDependencyTextByFileName(Map<String, String> dependencyTextByFileName) {
		this.dependencyTextByFileName = dependencyTextByFileName;
		generatedClasses = null;
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
			throw new UnexpectedError(e);
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

	protected void generateClasses() {
		if (text == null) {
			generatedClasses = Collections.emptyList();
			return;
		}
		generatedClasses = null;
		try {
			File directory = MiscUtils.createTemporaryDirectory();
			File mainFile = new File(directory, "main." + getClass().getSimpleName().toLowerCase());
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
				MiscUtils.write(metaSchemaFile, MiscUtils.read(XSD.class.getResourceAsStream(metaSchemaFile.getName())),
						false);
				MiscUtils.write(metaXMLFile, MiscUtils.read(XSD.class.getResourceAsStream(metaXMLFile.getName())),
						false);
				MiscUtils.write(metaSchemaDTDFile,
						MiscUtils.read(XSD.class.getResourceAsStream(metaSchemaDTDFile.getName())), false);
				MiscUtils.write(metaSchemaDatatypesDTDFile,
						MiscUtils.read(XSD.class.getResourceAsStream(metaSchemaDatatypesDTDFile.getName())), false);
				for (Map.Entry<File, String> dependencyTextByFileEntry : dependencyTextByFile.entrySet()) {
					MiscUtils.write(dependencyTextByFileEntry.getKey(), dependencyTextByFileEntry.getValue(), false);
				}
				File sourceDirectory = MiscUtils.createTemporaryDirectory();
				try {
					runClassesGenerationTool(mainFile, metaSchemaFile, sourceDirectory);
					generatedClasses = MiscUtils.IN_MEMORY_COMPILER.compile(sourceDirectory);
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
					if (JESB.DEBUG) {
						ignore.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			throw new UnexpectedError(e);
		}
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		if (generatedClasses == null) {
			try {
				generateClasses();
			} catch (Throwable t) {
				throw new ValidationError("Failed to validate the " + getClass().getSimpleName(), t);
			}
		}
	}

}