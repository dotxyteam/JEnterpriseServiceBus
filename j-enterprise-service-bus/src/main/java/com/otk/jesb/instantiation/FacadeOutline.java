package com.otk.jesb.instantiation;

import java.util.List;
import java.util.stream.Collectors;

public class FacadeOutline {

	private Facade facade;

	public FacadeOutline(Facade facade) {
		this.facade = facade;
	}

	public boolean isFacadeConcrete() {
		return facade.isConcrete();
	}

	public Facade getFacade() {
		return facade;
	}

	public String express() {
		return facade.express();
	}

	public FacadeOutline getParent() {
		if (facade.getParent() == null) {
			return null;
		}
		return new FacadeOutline(facade.getParent());
	}

	public List<FacadeOutline> getChildren() {
		return facade.getChildren().stream().map(facadeChild -> new FacadeOutline(facadeChild))
				.collect(Collectors.toList());
	}
	
	
	public RootInstanceBuilderFacade getRootInstanceBuilderFacade() {
		Facade rootFacade = Facade.getRoot(facade);
		if(!(rootFacade instanceof RootInstanceBuilderFacade)) {
			return null;
		}
		return (RootInstanceBuilderFacade) rootFacade;
	}
	


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((facade == null) ? 0 : facade.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FacadeOutline other = (FacadeOutline) obj;
		if (facade == null) {
			if (other.facade != null)
				return false;
		} else if (!facade.equals(other.facade))
			return false;
		return true;
	}

	public String toString() {
		return facade.toString();
	}

}
