package com.otk.jesb.resource.builtin;

import com.otk.jesb.Structure;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
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
	private UpToDate<Class<?>> upToDateStructuredClass = new UpToDate<Class<?>>() {
		@Override
		protected Object retrieveLastVersionIdentifier() {
			return (structure != null) ? MiscUtils.serialize(structure) : null;
		}

		@Override
		protected Class<?> obtainLatest(Object versionIdentifier) {
			if (structure == null) {
				return null;
			} else {
				try {
					String className = STRUCTURED_CLASS_NAME_PREFIX + InstantiationUtils.toRelativeTypeNameVariablePart(
							MiscUtils.toDigitalUniqueIdentifier(SharedStructureModel.this));
					return (Class<?>) MiscUtils.IN_MEMORY_COMPILER.compile(className,
							structure.generateJavaTypeSourceCode(className));
				} catch (CompilationError e) {
					throw new PotentialError(e);
				}
			}
		}
	};

	public SharedStructureModel() {
	}

	public SharedStructureModel(String name) {
		super(name);
	}

	public Structure getStructure() {
		return structure;
	}

	public void setStructure(Structure structure) {
		this.structure = structure;
	}

	public Class<?> getStructuredClass() {
		try {
			return upToDateStructuredClass.get();
		} catch (VersionAccessException e) {
			throw new PotentialError(e);
		}
	}

	public Accessor<String> getStructuredClassNameAccessor() {
		return new StructuredClassNameAccessor(Reference.get(this));
	}

	@Override
	public void validate(boolean recursively) throws ValidationError {
		super.validate(recursively);
		if (recursively) {
			if (structure != null) {
				try {
					structure.validate(recursively);
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

	private static class StructuredClassNameAccessor extends Accessor<String> {

		private Reference<SharedStructureModel> modelReference;

		public StructuredClassNameAccessor(Reference<SharedStructureModel> modelReference) {
			this.modelReference = modelReference;
		}

		@Override
		public String get() {
			SharedStructureModel model = modelReference.resolve();
			if (model == null) {
				return null;
			}
			return model.getStructuredClass().getName();
		}
	}

}
