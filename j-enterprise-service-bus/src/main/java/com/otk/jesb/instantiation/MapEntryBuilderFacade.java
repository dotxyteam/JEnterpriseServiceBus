package com.otk.jesb.instantiation;

import java.util.List;
import java.util.stream.Collectors;

public class MapEntryBuilderFacade extends InstanceBuilderFacade {

	public MapEntryBuilderFacade(Facade parent, MapEntryBuilder mapEntryBuilder) {
		super(parent, mapEntryBuilder);
	}

	@Override
	public void setTypeName(String typeName) {
		throw new UnsupportedOperationException("Cannot change map entry type name");
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