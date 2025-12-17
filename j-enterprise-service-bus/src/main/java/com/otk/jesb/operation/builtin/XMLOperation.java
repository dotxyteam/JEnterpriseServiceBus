package com.otk.jesb.operation.builtin;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.Reference;
import com.otk.jesb.ValidationError;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.resource.builtin.XSD;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.solution.Step;

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

		private XSD getXSD(Solution solutionInstance) {
			return xsdReference.resolve(solutionInstance);
		}

		public Reference<XSD> getXsdReference() {
			return xsdReference;
		}

		public void setXsdReference(Reference<XSD> xsdReference, Solution solutionInstance) {
			this.xsdReference = xsdReference;
			tryToSelectValuesAutomatically(solutionInstance);
		}

		public String getRootElementName() {
			return rootElementName;
		}

		public void setRootElementName(String rootElementName) {
			this.rootElementName = rootElementName;
		}

		private void tryToSelectValuesAutomatically(Solution solutionInstance) {
			try {
				if (rootElementName == null) {
					XSD xsd = getXSD(solutionInstance);
					if (xsd != null) {
						List<XSD.RootElementDescriptor> rootElements = xsd.getRootElements(solutionInstance);
						if (rootElements.size() > 0) {
							rootElementName = rootElements.get(0).getName();
						}
					}
				}
			} catch (Throwable t) {
			}
		}

		public List<String> getRootElementNameOptions(Solution solutionInstance) {
			XSD xsd = getXSD(solutionInstance);
			if (xsd == null) {
				return Collections.emptyList();
			}
			return xsd.getRootElements(solutionInstance).stream().map(e -> e.getName()).collect(Collectors.toList());
		}

		protected XSD.RootElementDescriptor retrieveRootElement(Solution solutionInstance) {
			XSD xsd = getXSD(solutionInstance);
			if (xsd == null) {
				return null;
			}
			return xsd.getRootElements(solutionInstance).stream().filter(e -> e.getName().equals(rootElementName))
					.findFirst().orElse(null);
		}

		@Override
		public void validate(boolean recursively, Solution solutionInstance, Plan plan, Step step)
				throws ValidationError {
			if (getXSD(solutionInstance) == null) {
				throw new ValidationError("Failed to resolve the XSD reference");
			}
			if (retrieveRootElement(solutionInstance) == null) {
				throw new ValidationError("Invalid root element name '" + rootElementName + "'");
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
