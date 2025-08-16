package com.otk.jesb.activation.builtin;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.ValidationError;
import com.otk.jesb.Variant;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.solution.Plan;

import xy.reflect.ui.info.ResourcePath;

public class WatchFileSystem extends Activator {

	private Variant<String> baseDircetoryPathVariant = new Variant<String>(String.class, ".");
	private Variant<String> patternVariant = new Variant<String>(String.class, "*");
	private Variant<Boolean> subDirectoriesWatchedRecursivelyVariant = new Variant<Boolean>(Boolean.class, false);
	private Variant<ResourceKind> watchedResourceKindVariant = new Variant<ResourceKind>(ResourceKind.class,
			ResourceKind.ANY);
	private Variant<Boolean> creationWatchedVariant = new Variant<Boolean>(Boolean.class, true);
	private Variant<Boolean> deletionWatchedVariant = new Variant<Boolean>(Boolean.class, true);
	private Variant<Boolean> modificationWatchedVariant = new Variant<Boolean>(Boolean.class, true);

	private ActivationHandler activationHandler;
	private WatchService watchService;
	private Thread thread;

	public Variant<String> getBaseDircetoryPathVariant() {
		return baseDircetoryPathVariant;
	}

	public void setBaseDircetoryPathVariant(Variant<String> baseDircetoryPathVariant) {
		this.baseDircetoryPathVariant = baseDircetoryPathVariant;
	}

	public Variant<String> getPatternVariant() {
		return patternVariant;
	}

	public void setPatternVariant(Variant<String> patternVariant) {
		this.patternVariant = patternVariant;
	}

	public Variant<Boolean> getSubDirectoriesWatchedRecursivelyVariant() {
		return subDirectoriesWatchedRecursivelyVariant;
	}

	public void setSubDirectoriesWatchedRecursivelyVariant(Variant<Boolean> subDirectoriesWatchedRecursivelyVariant) {
		this.subDirectoriesWatchedRecursivelyVariant = subDirectoriesWatchedRecursivelyVariant;
	}

	public Variant<ResourceKind> getWatchedResourceKindVariant() {
		return watchedResourceKindVariant;
	}

	public void setWatchedResourceKindVariant(Variant<ResourceKind> watchedResourceKindVariant) {
		this.watchedResourceKindVariant = watchedResourceKindVariant;
	}

	public Variant<Boolean> getCreationWatchedVariant() {
		return creationWatchedVariant;
	}

	public void setCreationWatchedVariant(Variant<Boolean> creationWatchedVariant) {
		this.creationWatchedVariant = creationWatchedVariant;
	}

	public Variant<Boolean> getDeletionWatchedVariant() {
		return deletionWatchedVariant;
	}

	public void setDeletionWatchedVariant(Variant<Boolean> deletionWatchedVariant) {
		this.deletionWatchedVariant = deletionWatchedVariant;
	}

	public Variant<Boolean> getModificationWatchedVariant() {
		return modificationWatchedVariant;
	}

	public void setModificationWatchedVariant(Variant<Boolean> modificationWatchedVariant) {
		this.modificationWatchedVariant = modificationWatchedVariant;
	}

	@Override
	public Class<?> getInputClass() {
		return FileSystemEvent.class;
	}

	@Override
	public Class<?> getOutputClass() {
		return null;
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler) throws Exception {
		this.activationHandler = activationHandler;
		Path nioBaseDircetoryPath = Paths.get(baseDircetoryPathVariant.getValue());
		watchService = FileSystems.getDefault().newWatchService();
		boolean subDirectoriesWatchedRecursively = subDirectoriesWatchedRecursivelyVariant.getValue();
		if (subDirectoriesWatchedRecursively) {
			registerDirectoriesRecursively(nioBaseDircetoryPath, watchService, creationWatchedVariant.getValue(),
					deletionWatchedVariant.getValue(), modificationWatchedVariant.getValue());
		} else {
			registerDirectory(watchService, nioBaseDircetoryPath, creationWatchedVariant.getValue(),
					deletionWatchedVariant.getValue(), modificationWatchedVariant.getValue());
		}
		thread = new Thread("Worker[of=" + WatchFileSystem.this + "]") {

			@Override
			public void run() {
				PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + patternVariant.getValue());
				ResourceKind watchedResourceKind = watchedResourceKindVariant.getValue();
				while (true) {
					try {
						if (isInterrupted()) {
							break;
						}
						WatchKey key;
						try {
							key = watchService.take();
						} catch (ClosedWatchServiceException e) {
							break;
						}
						for (WatchEvent<?> event : key.pollEvents()) {
							WatchEvent.Kind<?> eventKind = event.kind();
							Path nioPath = (Path) event.context();
							Path resolvedNioPath = nioBaseDircetoryPath.resolve(nioPath);
							System.out.println(eventKind + ": " + resolvedNioPath);
							if (subDirectoriesWatchedRecursively) {
								if (eventKind == StandardWatchEventKinds.ENTRY_CREATE) {
									if (Files.isDirectory(resolvedNioPath, LinkOption.NOFOLLOW_LINKS)) {
										try {
											registerDirectoriesRecursively(resolvedNioPath, watchService,
													creationWatchedVariant.getValue(),
													deletionWatchedVariant.getValue(),
													modificationWatchedVariant.getValue());
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
								}
							}
							if (pathMatcher.matches(nioPath)) {
								ResourceKind eventResourceKind = Files.isDirectory(resolvedNioPath)
										? ResourceKind.DIRECTORY
										: ResourceKind.FILE;
								if ((eventResourceKind == watchedResourceKind)
										|| (watchedResourceKind == ResourceKind.ANY)) {

									String path = resolvedNioPath.toString();
									boolean creationEvent = eventKind == StandardWatchEventKinds.ENTRY_CREATE;
									boolean deletionEvent = eventKind == StandardWatchEventKinds.ENTRY_DELETE;
									boolean modificationEvent = eventKind == StandardWatchEventKinds.ENTRY_MODIFY;
									activationHandler.trigger(new FileSystemEvent(path, eventResourceKind,
											creationEvent, deletionEvent, modificationEvent));
								}
							}
						}
						if (!key.reset()) {
							break;
						}
					} catch (Throwable t) {
						if (t instanceof InterruptedException) {
							break;
						} else {
							t.printStackTrace();
						}
					}
				}
			}

		};
		thread.start();
	}

	@Override
	public void finalizeAutomaticTrigger() throws Exception {
		watchService.close();
		while (thread.isAlive()) {
			thread.interrupt();
			Thread.sleep(100);
		}
		this.activationHandler = null;
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		return activationHandler != null;
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return true;
	}

	private void registerDirectory(WatchService watchService, Path nioDircetoryPath, boolean creationWatched,
			boolean deletionWatched, boolean modificationWatched) throws IOException {
		List<WatchEvent.Kind<Path>> eventKinds = new ArrayList<WatchEvent.Kind<Path>>();
		if (creationWatched) {
			eventKinds.add(StandardWatchEventKinds.ENTRY_CREATE);
		}
		if (deletionWatched) {
			eventKinds.add(StandardWatchEventKinds.ENTRY_DELETE);
		}
		if (modificationWatched) {
			eventKinds.add(StandardWatchEventKinds.ENTRY_MODIFY);
		}
		nioDircetoryPath.register(watchService, eventKinds.toArray(new WatchEvent.Kind[eventKinds.size()]));
	}

	private void registerDirectoriesRecursively(final Path root, WatchService watchService, boolean creationWatched,
			boolean deletionWatched, boolean modificationWatched) throws IOException {
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path nioDirectory, BasicFileAttributes attrs) throws IOException {
				registerDirectory(watchService, nioDirectory, creationWatched, deletionWatched, modificationWatched);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public void validate(boolean recursively, Plan plan) throws ValidationError {
		String baseDircetoryPath = baseDircetoryPathVariant.getValue();
		if ((baseDircetoryPath == null) || baseDircetoryPath.isEmpty()) {
			throw new ValidationError("Base directory path not provided");
		}
		if (getEnabledVariant().getValue()) {
			if (!Files.isDirectory(Paths.get(baseDircetoryPath))) {
				throw new ValidationError(
						"Base directory path does not point to a valid directory: '" + baseDircetoryPath + "'");
			}
		}
		if (!creationWatchedVariant.getValue() && !deletionWatchedVariant.getValue()
				&& !modificationWatchedVariant.getValue()) {
			throw new ValidationError("No enabled event (creation, deletion, modification)");
		}
	}

	public static enum ResourceKind {
		FILE, DIRECTORY, ANY
	}

	public static class FileSystemEvent {

		private String path;
		private ResourceKind resourceKind;
		private boolean creationEvent;
		private boolean deletionEvent;
		private boolean modificationEvent;

		public FileSystemEvent(String path, ResourceKind resourceKind, boolean creationEvent, boolean deletionEvent,
				boolean modificationEvent) {
			this.path = path;
			this.resourceKind = resourceKind;
			this.creationEvent = creationEvent;
			this.deletionEvent = deletionEvent;
			this.modificationEvent = modificationEvent;
		}

		public String getPath() {
			return path;
		}

		public ResourceKind getResourceKind() {
			return resourceKind;
		}

		public boolean isCreationEvent() {
			return creationEvent;
		}

		public boolean isDeletionEvent() {
			return deletionEvent;
		}

		public boolean isModificationEvent() {
			return modificationEvent;
		}

	}

	public static class Metadata implements ActivatorMetadata {

		@Override
		public ResourcePath getActivatorIconImagePath() {
			return new ResourcePath(ResourcePath
					.specifyClassPathResourceLocation(WatchFileSystem.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Activator> getActivatorClass() {
			return WatchFileSystem.class;
		}

		@Override
		public String getActivatorName() {
			return "Watch File System";
		}

	}
}
