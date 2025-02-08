package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

public interface Facade {

	List<Facade> getChildren();

	boolean isConcrete();

	void setConcrete(boolean b);

	Object getUnderlying();

	static List<Facade> getAncestors(Facade facade) {
		List<Facade> result = new ArrayList<Facade>();
		if (facade instanceof InstanceBuilderFacade) {
			InstanceBuilderFacade specificFacade = (InstanceBuilderFacade) facade;
			if (specificFacade.getParent() != null) {
				result.add(specificFacade.getParent());
				result.addAll(getAncestors(specificFacade.getParent()));
			}
		} else if (facade instanceof FieldInitializerFacade) {
			FieldInitializerFacade specificFacade = (FieldInitializerFacade) facade;
			Facade facadeParent = specificFacade.getParent();
			result.add(facadeParent);
			result.addAll(getAncestors(facadeParent));
		} else if (facade instanceof ParameterInitializerFacade) {
			ParameterInitializerFacade specificFacade = (ParameterInitializerFacade) facade;
			Facade facadeParent = specificFacade.getParent();
			result.add(facadeParent);
			result.addAll(getAncestors(facadeParent));
		} else if (facade instanceof ListItemInitializerFacade) {
			ListItemInitializerFacade specificFacade = (ListItemInitializerFacade) facade;
			Facade facadeParent = specificFacade.getParent();
			result.add(facadeParent);
			result.addAll(getAncestors(facadeParent));
		} else if (facade instanceof InitializationSwitchFacade) {
			InitializationSwitchFacade specificFacade = (InitializationSwitchFacade) facade;
			if (specificFacade.getParent() != null) {
				result.add(specificFacade.getParent());
				result.addAll(getAncestors(specificFacade.getParent()));
			}
		} else if (facade instanceof InitializationCaseFacade) {
			InitializationCaseFacade specificFacade = (InitializationCaseFacade) facade;
			if (specificFacade.getParent() != null) {
				result.add(specificFacade.getParent());
				result.addAll(getAncestors(specificFacade.getParent()));
			}
		} else {
			throw new AssertionError();
		}
		return result;
	}

	static Facade get(Object node, Facade parentFacade) {
		if (node instanceof MapEntryBuilder) {
			return new MapEntryBuilderFacade(parentFacade, (MapEntryBuilder) node);
		} else if (node instanceof InstanceBuilder) {
			return new InstanceBuilderFacade(parentFacade, (InstanceBuilder) node);
		} else if (node instanceof FieldInitializer) {
			return new FieldInitializerFacade((InstanceBuilderFacade) parentFacade,
					((FieldInitializer) node).getFieldName());
		} else if (node instanceof ParameterInitializer) {
			return new ParameterInitializerFacade((InstanceBuilderFacade) parentFacade,
					((ParameterInitializer) node).getParameterPosition());
		} else if (node instanceof ListItemInitializer) {
			return new ListItemInitializerFacade((InstanceBuilderFacade) parentFacade,
					((InstanceBuilderFacade) parentFacade).getUnderlying().getListItemInitializers()
							.indexOf((ListItemInitializer) node));
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

}