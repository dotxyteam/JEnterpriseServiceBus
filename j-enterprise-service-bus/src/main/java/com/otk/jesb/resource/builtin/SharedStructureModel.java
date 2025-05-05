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
					String className = SharedStructureModel.class.getPackage().getName() + ".SharedStructure"
							+ MiscUtils.getDigitalUniqueIdentifier(SharedStructureModel.this);
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
