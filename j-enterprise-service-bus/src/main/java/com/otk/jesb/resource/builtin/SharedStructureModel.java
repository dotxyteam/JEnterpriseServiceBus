package com.otk.jesb.resource.builtin;

import com.otk.jesb.Structure;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.Structure.Structured;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.resource.Resource;
import com.otk.jesb.resource.ResourceMetadata;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;

import xy.reflect.ui.info.ResourcePath;

public class SharedStructureModel extends Resource {

	private static final String STRUCTURED_CLASS_NAME_PREFIX = SharedStructureModel.class.getPackage().getName()
			+ ".SharedStructure";

	public static boolean isStructuredClass(Class<?> c) {
		return c.getName().startsWith(STRUCTURED_CLASS_NAME_PREFIX);
	}

	public static SharedStructureModel getFromStructuredClass(Class<?> c) {
		if(!isStructuredClass(c)) {
			throw new UnexpectedError();
		}
		String digitalUniqueIdentifier = c.getName().substring(STRUCTURED_CLASS_NAME_PREFIX.length());
		Object object = MiscUtils.fromFromDigitalUniqueIdentifier(digitalUniqueIdentifier);
		if(!(object instanceof SharedStructureModel)) {
			throw new UnexpectedError();
		}
		return (SharedStructureModel)object;
	}

	private Structure structure = new ClassicStructure();
	private UpToDate<Class<? extends Structured>> upToDateStructuredClass = new UpToDate<Class<? extends Structured>>() {
		@Override
		protected Object retrieveLastModificationIdentifier() {
			return (structure != null) ? MiscUtils.serialize(structure) : null;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<? extends Structured> obtainLatest() {
			if (structure == null) {
				return null;
			} else {
				try {
					String className = STRUCTURED_CLASS_NAME_PREFIX
							+ MiscUtils.toDigitalUniqueIdentifier(SharedStructureModel.this);
					return (Class<? extends Structured>) MiscUtils.IN_MEMORY_COMPILER.compile(className,
							structure.generateJavaTypeSourceCode(className));
				} catch (CompilationError e) {
					throw new UnexpectedError(e);
				}
			}
		}
	};

	public SharedStructureModel() {
		this(SharedStructureModel.class.getSimpleName() + MiscUtils.getDigitalUniqueIdentifier());
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

	public Class<? extends Structured> getStructuredClass() {
		return upToDateStructuredClass.get();
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

}
