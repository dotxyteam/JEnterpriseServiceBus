package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.util.MiscUtils;

public abstract class Facade {

	public abstract String express();

	public abstract Facade getParent();

	public abstract List<? extends Facade> getChildren();

	public abstract boolean isConcrete();

	public abstract void setConcrete(boolean b);

	public abstract Object getUnderlying();

	public abstract List<VariableDeclaration> getAdditionalVariableDeclarations();

	public abstract Class<?> getFunctionReturnType(InstantiationFunction function);

	public abstract void validate(boolean recursively, List<VariableDeclaration> variableDeclarations)
			throws ValidationError;

	public static List<Facade> getAncestors(Facade facade) {
		List<Facade> result = new ArrayList<Facade>();
		Facade parentFacade;
		while ((parentFacade = facade.getParent()) != null) {
			result.add(parentFacade);
			facade = parentFacade;
		}
		return result;
	}

	public static Facade getRoot(Facade facade) {
		List<Facade> ancestors = getAncestors(facade);
		if (ancestors.size() == 0) {
			return facade;
		}
		return ancestors.get(ancestors.size() - 1);
	}

	public static Facade get(Object node, Facade parentFacade) {
		if (node instanceof RootInstanceBuilder) {
			if (parentFacade != null) {
				throw new UnexpectedError();
			}
			return new RootInstanceBuilderFacade((RootInstanceBuilder) node);
		} else if (node instanceof MapEntryBuilder) {
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
			throw new UnexpectedError();
		}
	}

	public static boolean same(Facade facade1, Facade facade2) {
		if (facade1.getClass() != facade2.getClass()) {
			return false;
		}
		if (!facade1.toString().equals(facade2.toString())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getParent() == null) ? 0 : getParent().hashCode());
		result = prime * result + getClass().hashCode();
		result = prime * result + toString().hashCode();
		result = prime * result + ((getUnderlying() == null) ? 0 : getUnderlying().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Facade)) {
			return false;
		}
		Facade otherFacade = (Facade) obj;
		if (!MiscUtils.equalsOrBothNull(getParent(), otherFacade.getParent())) {
			return false;
		}
		if (!same(this, otherFacade)) {
			return false;
		}
		if (isConcrete() != otherFacade.isConcrete()) {
			return false;
		}
		if (isConcrete()) {
			if (!MiscUtils.equalsOrBothNull(getUnderlying(), otherFacade.getUnderlying())) {
				return false;
			}
		}
		return true;
	}

}