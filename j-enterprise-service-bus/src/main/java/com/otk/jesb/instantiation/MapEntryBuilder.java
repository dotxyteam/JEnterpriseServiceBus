package com.otk.jesb.instantiation;

import java.util.Map;

import xy.reflect.ui.info.type.iterable.map.StandardMapEntry;

/**
 * Allows to manage the instantiation of a standard {@link Map} entry.
 * 
 * @author olitank
 *
 */
public class MapEntryBuilder extends InstanceBuilder {

	public MapEntryBuilder() {
		super(StandardMapEntry.class.getName());
	}

}