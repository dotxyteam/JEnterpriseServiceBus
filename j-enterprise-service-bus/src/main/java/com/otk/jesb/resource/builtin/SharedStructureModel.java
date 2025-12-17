package com.otk.jesb.resource.builtin;

import com.otk.jesb.Structure;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.PotentialError;
import com.otk.jesb.Reference;
import com.otk.jesb.util.Accessor;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class SharedStructureModel extends Resource {

	private static final String STRUCTURED_CLASS_NAME_PREFIX = SharedStructureModel.class.getPackage().getName()
			+ ".SharedStructure";

	public static boolean isStructuredClass(Class<?> c) {
		return c.getName().startsWith(STRUCTURED_CLASS_NAME_PREFIX);
	}

	public static SharedStructureModel getFromStructuredClass(Class<?> c) {
		if (!isStructuredClass(c)) {
			throw new UnexpectedError();
		}
		String digitalUniqueIdentifier = c.getName().substring(STRUCTURED_CLASS_NAME_PREFIX.length());
		Object object = MiscUtils.fromFromDigitalUniqueIdentifier(digitalUniqueIdentifier);
		if (!(object instanceof SharedStructureModel)) {
			throw new UnexpectedError();
		}
		return (SharedStructureModel) object;
	}

	private Structure structure = new ClassicStructure();
	private UpToDate<Solution, Class<?>> upToDateStructuredClass = new UpToDate<Solution, Class<?>>() {
		@Override
		protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
			return (structure != null) ? MiscUtils.serialize(structure, solutionInstance.getRuntime().getXstream())
					: null;
		}

		@Override
		protected Class<?> obtainLatest(Solution solutionInstance, Object versionIdentifier) {
			if (structure == null) {
				return null;
			} else {
				try {
					String className = STRUCTURED_CLASS_NAME_PREFIX + InstantiationUtils.toRelativeTypeNameVariablePart(
							MiscUtils.toDigitalUniqueIdentifier(SharedStructureModel.this));
					return (Class<?>) solutionInstance.getRuntime().getInMemoryCompiler().compile(className,
							structure.generateJavaTypeSourceCode(className, solutionInstance));
				} catch (CompilationError e) {
					throw new PotentialError(e);
				}
			}
		}
	};

	public SharedStructureModel(String name) {
		super(name);
	}

	public SharedStructureModel() {
		super();
	}

	public Structure getStructure() {
		return structure;
	}

	public void setStructure(Structure structure) {
		this.structure = structure;
	}

	public Class<?> getStructuredClass(Solution solutionInstance) {
		try {
			return upToDateStructuredClass.get(solutionInstance);
		} catch (VersionAccessException e) {
			throw new PotentialError(e);
		}
	}

	public Accessor<Solution, String> getStructuredClassNameAccessor(Solution solutionInstance) {
		return new StructuredClassNameAccessor(Reference.get(this, solutionInstance));
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
		super.validate(recursively, solutionInstance);
		if (recursively) {
			if (structure != null) {
				try {
					structure.validate(recursively, solutionInstance);
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate the structure", e);
				}
			}
		}
	}

	public static class Metadata implements ResourceMetadata {

		@Override
		public ResourcePath getResourceIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(SharedStructureModel.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Resource> getResourceClass() {
			return SharedStructureModel.class;
		}

		@Override
		public String getResourceTypeName() {
			return "Shared Structure";
		}

	}

	private static class StructuredClassNameAccessor extends Accessor<Solution, String> {

		private Reference<SharedStructureModel> modelReference;

		public StructuredClassNameAccessor(Reference<SharedStructureModel> modelReference) {
			this.modelReference = modelReference;
		}

		@Override
		public String get(Solution solutionInstance) {
			SharedStructureModel model = modelReference.resolve(solutionInstance);
			if (model == null) {
				return null;
			}
			return model.getStructuredClass(solutionInstance).getName();
		}
	}

}
