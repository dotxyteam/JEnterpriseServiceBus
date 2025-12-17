package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import com.otk.jesb.solution.Asset;
import com.otk.jesb.solution.Folder;
import com.otk.jesb.solution.Solution;

/**
 * This class allows to store the access path to an {@link Asset} of a given
 * type.
 * 
 * @author olitank
 *
 * @param <T> SPecific type of asset.
 */
public class Reference<T extends Asset> {

	private static final String PATH_SEPARATOR = " > ";

	private String path;

	private Class<T> assetClass;
	private BiPredicate<T, Solution> assetFilter;
	private BiConsumer<String, Solution> newPathValidator;

	public Reference(Class<T> assetClass, BiPredicate<T, Solution> assetFilter,
			BiConsumer<String, Solution> newPathValidator) {
		this.assetClass = assetClass;
		this.assetFilter = assetFilter;
		this.newPathValidator = newPathValidator;
	}

	public Reference() {
		this(null);
	}

	public Reference(Class<T> assetClass) {
		this(assetClass, null, null);
	}

	public Class<T> getAssetClass() {
		return assetClass;
	}

	public void setAssetClass(Class<T> assetClass) {
		this.assetClass = assetClass;
	}

	public BiPredicate<T, Solution> getAssetFilter() {
		return assetFilter;
	}

	public void setAssetFilter(BiPredicate<T, Solution> assetFilter) {
		this.assetFilter = assetFilter;
	}

	public BiConsumer<String, Solution> getNewPathValidator() {
		return newPathValidator;
	}

	public void setNewPathValidator(BiConsumer<String, Solution> newPathValidator) {
		this.newPathValidator = newPathValidator;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path, Solution solutionInstance) {
		if (newPathValidator != null) {
			newPathValidator.accept(path, solutionInstance);
		}
		this.path = path;
	}

	public List<String> getPathOptions(Solution solutionInstance) {
		List<String> result = new ArrayList<String>();
		for (Asset asset : solutionInstance.getContents()) {
			result.addAll(findOptionsFrom(asset, asset.getName(), solutionInstance));
		}
		return result;
	}

	private List<String> findOptionsFrom(Asset asset, String assetPath, Solution solutionInstance) {
		List<String> result = new ArrayList<String>();
		if (assetClass.isInstance(asset)
				&& ((assetFilter == null) || assetFilter.test(assetClass.cast(asset), solutionInstance))) {
			result.add(assetPath);
		}
		if (asset instanceof Folder) {
			for (Asset childAsset : ((Folder) asset).getContents()) {
				result.addAll(findOptionsFrom(childAsset, assetPath + PATH_SEPARATOR + childAsset.getName(),
						solutionInstance));
			}
		}
		return result;
	}

	public T resolve(Solution solutionInstance) {
		if (path == null) {
			return null;
		}
		for (Asset asset : solutionInstance.getContents()) {
			T result = resolveFrom(asset, asset.getName());
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private T resolveFrom(Asset asset, String assetPath) {
		if (assetPath.equals(path)) {
			if (assetClass.isInstance(asset)) {
				return assetClass.cast(asset);
			}
		}
		if (path.startsWith(assetPath + PATH_SEPARATOR) && (asset instanceof Folder)) {
			for (Asset childAsset : ((Folder) asset).getContents()) {
				T result = resolveFrom(childAsset, assetPath + PATH_SEPARATOR + childAsset.getName());
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Asset> Reference<T> get(T asset, Solution solutionInstance) {
		for (String pathOption : new Reference<T>((Class<T>) asset.getClass()).getPathOptions(solutionInstance)) {
			Reference<T> candidate = new Reference<T>((Class<T>) asset.getClass());
			candidate.setPath(pathOption, solutionInstance);
			if (candidate.resolve(solutionInstance) == asset) {
				return candidate;
			}
		}
		return null;
	}

}
