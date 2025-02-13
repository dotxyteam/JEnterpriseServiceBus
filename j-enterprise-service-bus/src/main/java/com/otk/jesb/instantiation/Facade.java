package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

public interface Facade {

	Facade getParent();

	List<? extends Facade> getChildren();

	boolean isConcrete();

	void setConcrete(boolean b);

	Object getUnderlying();

	static List<Facade> getAncestors(Facade facade) {
		List<Facade> result = new ArrayList<Facade>();
		Facade parentFacade;
		while ((parentFacade = facade.getParent()) != null) {
			result.add(parentFacade);
			facade = parentFacade;
		}
		return result;
	}

	static Facade get(Object node, Facade parentFacade) {
		if (node instanceof MapEntryBuilder) {
			return new MapEntryBuilderFacade(parentFacade, (MapEntryBuilder) node);
		} else if (node instanceof InstanceBuilder) {
			return new InstanceBuilderFacade(parentFacade, (InstanceBuilder) node);
		} else if (node instanceof FieldInitializer) {
			return new FieldInitializerFacade(parentFacade, ((FieldInitializer) node).getFieldName());
		} else if (node instanceof ParameterInitializer) {
			return new ParameterInitializerFacade(parentFacade, ((ParameterInitializer) node).getParameterPosition());
		} else if (node instanceof ListItemInitializer) {
			return new ListItemInitializerFacade(parentFacade, ((ListItemInitializer) node).getIndex());
		} else if (node instanceof InitializationSwitch) {
			return new InitializationSwitchFacade(parentFacade, (InitializationSwitch) node);
		} else if (node instanceof InitializationCase) {
			return new InitializationCaseFacade((InitializationSwitchFacade) parentFacade,
					((InitializationSwitchFacade) parentFacade).getUnderlying()
							.findCondition((InitializationCase) node),
					(InitializationCase) node);
		} else {
			throw new AssertionError();
		}
	}

	static boolean same(Facade facade1, Facade facade2) {
		if (facade1.getClass() != facade2.getClass()) {
			return false;
		}
		if (!facade1.toString().equals(facade2.toString())) {
			return false;
		}
		return true;
	}

}