package com.otk.jesb.operation.builtin;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.Reference;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.SimpleElement;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.resource.builtin.XSD;
import com.otk.jesb.resource.builtin.XSD.RootElementDescriptor;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;

public abstract class XMLOperation implements Operation {

	private Class<?> rootElementClass;

	public XMLOperation(Class<?> rootElementClass) {
		this.rootElementClass = rootElementClass;
	}

	public Class<?> getRootElementClass() {
		return rootElementClass;
	}

	public static abstract class Builder<T extends XMLOperation> implements OperationBuilder<T> {

		private Reference<XSD> xsdReference = new Reference<XSD>(XSD.class);
		private String rootElementName;
		protected UpToDateDocumentObjectClass upToDateDocumentObjectClass = new UpToDateDocumentObjectClass();

		private XSD getXSD() {
			return xsdReference.resolve();
		}

		public Reference<XSD> getXsdReference() {
			return xsdReference;
		}

		public void setXsdReference(Reference<XSD> xsdReference) {
			this.xsdReference = xsdReference;
			tryToSelectValuesAutomatically();
		}

		public String getRootElementName() {
			return rootElementName;
		}

		public void setRootElementName(String rootElementName) {
			this.rootElementName = rootElementName;
		}

		private void tryToSelectValuesAutomatically() {
			try {
				if (rootElementName == null) {
					XSD xsd = getXSD();
					if (xsd != null) {
						List<XSD.RootElementDescriptor> rootElements = xsd.getRootElements();
						if (rootElements.size() > 0) {
							rootElementName = rootElements.get(0).getName();
						}
					}
				}
			} catch (Throwable t) {
			}
		}

		public List<String> getRootElementNameOptions() {
			XSD xsd = getXSD();
			if (xsd == null) {
				return Collections.emptyList();
			}
			return xsd.getRootElements().stream().map(e -> e.getName()).collect(Collectors.toList());
		}

		protected XSD.RootElementDescriptor retrieveRootElement() {
			XSD xsd = getXSD();
			if (xsd == null) {
				return null;
			}
			return xsd.getRootElements().stream().filter(e -> e.getName().equals(rootElementName)).findFirst()
					.orElse(null);
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (getXSD() == null) {
				throw new ValidationError("Failed to resolve the XSD reference");
			}
			if (retrieveRootElement() == null) {
				throw new ValidationError("Invalid root element name '" + rootElementName + "'");
			}
		}

		protected class UpToDateDocumentObjectClass extends UpToDate<Class<?>> {

			@Override
			protected Object retrieveLastVersionIdentifier() {
				RootElementDescriptor rootElementDescriptor = retrieveRootElement();
				if (rootElementDescriptor == null) {
					return null;
				}
				return rootElementDescriptor.retrieveClass();
			}

			@Override
			protected Class<?> obtainLatest(Object versionIdentifier) throws VersionAccessException {
				RootElementDescriptor rootElementDescriptor = retrieveRootElement();
				if (rootElementDescriptor == null) {
					return null;
				}
				ClassicStructure resultStructure = new ClassicStructure();
				{
					SimpleElement rootElement = new SimpleElement();
					rootElement.setName(getRootElementName());
					rootElement.setTypeName(rootElementDescriptor.retrieveClass().getName());
					resultStructure.getElements().add(rootElement);
				}
				String className = XMLOperation.class.getName() + "Document"
						+ MiscUtils.toDigitalUniqueIdentifier(this);
				try {
					return MiscUtils.IN_MEMORY_COMPILER.compile(className,
							resultStructure.generateJavaTypeSourceCode(className));
				} catch (CompilationError e) {
					throw new UnexpectedError(e);
				}
			}

		}

	}

	public static class SourceDocument {
		public final String text;

		public SourceDocument(String text) {
			this.text = text;
		}

	}

}
