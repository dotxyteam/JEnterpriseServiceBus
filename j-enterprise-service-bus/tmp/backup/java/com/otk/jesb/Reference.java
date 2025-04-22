package com.otk.jesb;

import java.util.ArrayList;
import java.util.List;

public class Reference<T extends Asset> {

	private static final String PATH_SEPARATOR = " > ";

	private String path;
	private Class<T> assetClass;

	public Reference(Class<T> assetClass) {
		this.assetClass = assetClass;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public List<String> getPathOptions() {
		List<String> result = new ArrayList<String>();
		for (Asset asset : Solution.INSTANCE.getContents()) {
			result.addAll(findOptionsFrom(asset, asset.getName()));
		}
		return result;
	}

	private List<String> findOptionsFrom(Asset asset, String assetPath) {
		List<String> result = new ArrayList<String>();
		if (assetClass.isInstance(asset)) {
			result.add(assetPath);
		}
		if (asset instanceof Folder) {
			for (Asset childAsset : ((Folder) asset).getContents()) {
				result.addAll(findOptionsFrom(childAsset, assetPath + PATH_SEPARATOR + childAsset.getName()));
			}
		}
		return result;
	}

	public T resolve() {
		if(path == null) {
			return null;
		}
		for (Asset asset : Solution.INSTANCE.getContents()) {
			T result = resolveFrom(asset, asset.getName());
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private T resolveFrom(Asset asset, String assetPath) {
		if (assetPath.equals(path)) {
			return assetClass.cast(asset);
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
	public static <T extends Asset> Reference<T> get(T asset){
		for(String pathOption: new Reference<T>((Class<T>) asset.getClass()).getPathOptions()) {
			Reference<T> candidate = new Reference<T>((Class<T>) asset.getClass());
			candidate.setPath(pathOption);
			if(candidate.resolve() == asset) {
				return candidate;
			}
		}
		return null;
	}

}
