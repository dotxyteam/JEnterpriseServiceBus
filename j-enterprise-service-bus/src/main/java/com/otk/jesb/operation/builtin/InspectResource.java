package com.otk.jesb.operation.builtin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import com.otk.jesb.ValidationError;
import com.otk.jesb.activation.builtin.WatchFileSystem;
import com.otk.jesb.activation.builtin.WatchFileSystem.ResourceKind;
import com.otk.jesb.instantiation.InstantiationContext;
import com.otk.jesb.instantiation.RootInstanceBuilder;
import com.otk.jesb.operation.Operation;
import com.otk.jesb.operation.OperationBuilder;
import com.otk.jesb.operation.OperationMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.solution.Step;

import xy.reflect.ui.info.ResourcePath;

public class InspectResource implements Operation {

	private String path;

	public InspectResource(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	@Override
	public Object execute() throws Throwable {
		Path nioPath = Paths.get(path);
		String absolutePath = nioPath.toAbsolutePath().toString();
		String name = nioPath.getFileName().toString();
		boolean existing = Files.exists(nioPath, LinkOption.NOFOLLOW_LINKS);
		WatchFileSystem.ResourceKind resourceKind = existing ? WatchFileSystem.ResourceKind.get(nioPath) : null;
		String owner;
		try {
			owner = existing ? Files.getOwner(nioPath, LinkOption.NOFOLLOW_LINKS).getName() : null;
		} catch (UnsupportedOperationException | IOException e) {
			owner = null;
		}
		boolean readable = existing ? Files.isReadable(nioPath) : false;
		boolean writable = existing ? Files.isWritable(nioPath) : false;
		boolean executable = existing ? Files.isExecutable(nioPath) : false;
		boolean hidden;
		try {
			hidden = existing ? Files.isHidden(nioPath) : false;
		} catch (IOException e) {
			hidden = false;
		}
		boolean symbolicLink = existing ? Files.isSymbolicLink(nioPath) : false;
		String fileStoreName;
		try {
			fileStoreName = existing ? Files.getFileStore(nioPath).name() : null;
		} catch (IOException e) {
			fileStoreName = null;
		}
		Date lastModificationTimestamp;
		try {
			lastModificationTimestamp = existing
					? new Date(Files.getLastModifiedTime(nioPath, LinkOption.NOFOLLOW_LINKS).toMillis())
					: null;
		} catch (IOException e) {
			lastModificationTimestamp = null;
		}
		return new PathInpectionResult(absolutePath, name, existing, resourceKind, owner, readable, writable,
				executable, hidden, symbolicLink, fileStoreName, lastModificationTimestamp);
	}

	public static class PathInpectionResult {
		private String absolutePath;
		private String name;
		private WatchFileSystem.ResourceKind resourceKind;
		private String owner;
		private boolean existing;
		private boolean readable;
		private boolean writable;
		private boolean executable;
		private boolean hidden;
		private boolean symbolicLink;
		private String fileStoreName;
		private Date lastModificationTimestamp;

		public PathInpectionResult(String absolutePath, String name, boolean existing, ResourceKind resourceKind,
				String owner, boolean readable, boolean writable, boolean executable, boolean hidden,
				boolean symbolicLink, String fileStoreName, Date lastModificationTimestamp) {
			this.absolutePath = absolutePath;
			this.name = name;
			this.resourceKind = resourceKind;
			this.owner = owner;
			this.existing = existing;
			this.readable = readable;
			this.writable = writable;
			this.executable = executable;
			this.hidden = hidden;
			this.symbolicLink = symbolicLink;
			this.fileStoreName = fileStoreName;
			this.lastModificationTimestamp = lastModificationTimestamp;
		}

		public String getAbsolutePath() {
			return absolutePath;
		}

		public String getName() {
			return name;
		}

		public WatchFileSystem.ResourceKind getResourceKind() {
			return resourceKind;
		}

		public String getOwner() {
			return owner;
		}

		public boolean isExisting() {
			return existing;
		}

		public boolean isReadable() {
			return readable;
		}

		public boolean isWritable() {
			return writable;
		}

		public boolean isExecutable() {
			return executable;
		}

		public boolean isHidden() {
			return hidden;
		}

		public boolean isSymbolicLink() {
			return symbolicLink;
		}

		public String getFileStoreName() {
			return fileStoreName;
		}

		public Date getLastModificationTimestamp() {
			return lastModificationTimestamp;
		}

	}

	public static class Builder implements OperationBuilder<InspectResource> {

		private RootInstanceBuilder instanceBuilder = new RootInstanceBuilder(
				InspectResource.class.getSimpleName() + "Input", InspectResource.class.getName());

		public RootInstanceBuilder getInstanceBuilder() {
			return instanceBuilder;
		}

		public void setInstanceBuilder(RootInstanceBuilder instanceBuilder) {
			this.instanceBuilder = instanceBuilder;
		}

		@Override
		public InspectResource build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception {
			return (InspectResource) instanceBuilder.build(new InstantiationContext(context.getVariables(),
					context.getPlan().getValidationContext(context.getCurrentStep()).getVariableDeclarations()));
		}

		@Override
		public Class<?> getOperationResultClass(Plan currentPlan, Step currentStep) {
			return PathInpectionResult.class;
		}

		@Override
		public void validate(boolean recursively, Plan plan, Step step) throws ValidationError {
			if (recursively) {
				if (instanceBuilder != null) {
					try {
						instanceBuilder.getFacade().validate(recursively,
								plan.getValidationContext(step).getVariableDeclarations());
					} catch (ValidationError e) {
						throw new ValidationError("Failed to validate the input builder", e);
					}
				}
			}
		}

	}

	public static class Metadata implements OperationMetadata<InspectResource> {

		@Override
		public String getOperationTypeName() {
			return "Inspect Resource";
		}

		@Override
		public String getCategoryName() {
			return "File System";
		}

		@Override
		public Class<? extends OperationBuilder<InspectResource>> getOperationBuilderClass() {
			return Builder.class;
		}

		@Override
		public ResourcePath getOperationIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(InspectResource.class.getName().replace(".", "/") + ".png"));
		}
	}

}
