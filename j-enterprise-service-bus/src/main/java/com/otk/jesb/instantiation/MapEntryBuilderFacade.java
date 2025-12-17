package com.otk.jesb.instantiation;

import java.util.List;
import java.util.stream.Collectors;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.solution.Solution;

public class MapEntryBuilderFacade extends InstanceBuilderFacade {

	public MapEntryBuilderFacade(Facade parent, MapEntryBuilder mapEntryBuilder, Solution solutionInstance) {
		super(parent, mapEntryBuilder, solutionInstance);
	}

	@Override
	public void setTypeName(String typeName) {
		throw new UnexpectedError("Cannot change map entry type name");
	}

	@Override
	public MapEntryBuilder getUnderlying() {
		return (MapEntryBuilder) super.getUnderlying();
	}

	@Override
	public List<Facade> getChildren() {
		return super.getChildren().stream().filter(f -> !(f instanceof FieldInitializerFacade))
				.collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return "<MapEntry>";
	}

}