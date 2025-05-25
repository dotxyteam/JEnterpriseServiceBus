package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;

public class InitializationSwitchFacade extends Facade {

	private Facade parent;
	private InitializationSwitch underlying;

	public InitializationSwitchFacade(Facade parent, InitializationSwitch underlying) {
		this.parent = parent;
		this.underlying = underlying;
	}

	@Override
	public List<VariableDeclaration> getAdditionalVariableDeclarations(
			List<VariableDeclaration> baseVariableDeclarations) {
		return parent.getAdditionalVariableDeclarations(baseVariableDeclarations);
	}

	@Override
	public Class<?> getFunctionReturnType(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void validate(boolean recursively, List<VariableDeclaration> variableDeclarations) throws ValidationError {
		if (!isConcrete()) {
			return;
		}
		if (recursively) {
			for (Facade facade : getChildren()) {
				try {
					facade.validate(recursively, variableDeclarations);
				} catch (ValidationError e) {
					throw new ValidationError("Failed to validate '" + facade.toString() + "'", e);
				}
			}
		}
	}

	@Override
	public String express() {
		return null;
	}

	public static InitializationSwitchFacade install(Facade parent, int caseCount, List<Facade> initializerFacades) {
		if (caseCount < 1) {
			throw new IllegalArgumentException("Invalid number of cases: " + caseCount + "(must not be < 1)");
		}
		InitializationSwitch underlying = new InitializationSwitch();
		{
			for (int iCase = 0; iCase < caseCount; iCase++) {
				underlying.getInitializationCaseByCondition().put(InitializationCase.createDefaultCondition(),
						new InitializationCase());
			}
			underlying.setDefaultInitializationCase(new InitializationCase());
		}
		InitializationSwitchFacade result = new InitializationSwitchFacade(parent, underlying);
		result.importInitializerFacades(initializerFacades);
		parent.setConcrete(true);
		((InitializationCase) parent.getUnderlying()).getInitializationSwitches().add(underlying);
		return result;
	}

	public void importInitializerFacades(List<Facade> initializerFacades) {
		for (Facade initializerFacade : initializerFacades) {
			if (initializerFacade instanceof ParameterInitializerFacade) {
				ParameterInitializer initializer = ((ParameterInitializerFacade) initializerFacade).getUnderlying();
				if (!((InitializationCase) parent.getUnderlying()).getParameterInitializers().remove(initializer)) {
					throw new UnexpectedError();
				}
				for (InitializationCaseFacade caseFacade : getChildren()) {
					caseFacade.getUnderlying().getParameterInitializers().add(MiscUtils.copy(initializer));
				}
			} else if (initializerFacade instanceof FieldInitializerFacade) {
				((FieldInitializerFacade) initializerFacade).setConcrete(true);
				FieldInitializer initializer = ((FieldInitializerFacade) initializerFacade).getUnderlying();
				if (!((InitializationCase) parent.getUnderlying()).getFieldInitializers().remove(initializer)) {
					throw new UnexpectedError();
				}
				for (InitializationCaseFacade caseFacade : getChildren()) {
					caseFacade.getUnderlying().getFieldInitializers().add(MiscUtils.copy(initializer));
				}
			} else if (initializerFacade instanceof ListItemInitializerFacade) {
				((ListItemInitializerFacade) initializerFacade).setConcrete(true);
				ListItemInitializer initializer = ((ListItemInitializerFacade) initializerFacade).getUnderlying();
				if (!((InitializationCase) parent.getUnderlying()).getListItemInitializers().remove(initializer)) {
					throw new UnexpectedError();
				}
				for (InitializationCaseFacade caseFacade : getChildren()) {
					caseFacade.getUnderlying().getListItemInitializers().add(MiscUtils.copy(initializer));
				}
			} else if (initializerFacade instanceof InitializationSwitchFacade) {
				InitializationSwitch initializer = ((InitializationSwitchFacade) initializerFacade).getUnderlying();
				if (!((InitializationCase) parent.getUnderlying()).getInitializationSwitches().remove(initializer)) {
					throw new UnexpectedError();
				}
				for (InitializationCaseFacade caseFacade : getChildren()) {
					caseFacade.getUnderlying().getInitializationSwitches().add(MiscUtils.copy(initializer));
				}
			} else {
				throw new UnexpectedError();
			}
		}

	}

	public void addNewCase() {
		List<InitializationCaseFacade> children = getChildren();
		children.get(children.size() - 1).insertNewSibling();
	}

	@Override
	public List<InitializationCaseFacade> getChildren() {
		List<InitializationCaseFacade> result = new ArrayList<InitializationCaseFacade>();
		for (Map.Entry<InstantiationFunction, InitializationCase> caseEntry : underlying
				.getInitializationCaseByCondition().entrySet()) {
			result.add(new InitializationCaseFacade(this, caseEntry.getKey(), caseEntry.getValue()));
		}
		result.add(new InitializationCaseFacade(this, null, underlying.getDefaultInitializationCase()));
		return result;
	}

	@Override
	public Facade getParent() {
		return parent;
	}

	@Override
	public boolean isConcrete() {
		if (!parent.isConcrete()) {
			return false;
		}
		return ((InitializationCase) parent.getUnderlying()).getInitializationSwitches().contains(underlying);
	}

	@Override
	public void setConcrete(boolean b) {
		if (b == isConcrete()) {
			return;
		}
		if (b) {
			if (!parent.isConcrete()) {
				parent.setConcrete(true);
			}
			if (!((InitializationCase) parent.getUnderlying()).getInitializationSwitches().contains(underlying)) {
				((InitializationCase) parent.getUnderlying()).getInitializationSwitches().add(underlying);
			}
		} else {
			for (InitializationCaseFacade caseFacade : getChildren()) {
				caseFacade.setConcrete(false);
			}
			((InitializationCase) parent.getUnderlying()).getInitializationSwitches().remove(underlying);
		}
	}

	@Override
	public InitializationSwitch getUnderlying() {
		return underlying;
	}

	public List<Facade> getManagedInitializerFacades() {
		List<Facade> result = new ArrayList<Facade>();
		for (InitializationCaseFacade caseFacade : getChildren()) {
			for (Facade facade : caseFacade.getChildren()) {
				if (!facade.isConcrete()) {
					continue;
				}
				if (facade instanceof InitializationSwitchFacade) {
					for (Facade subSwitchFacade : ((InitializationSwitchFacade) facade)
							.getManagedInitializerFacades()) {
						if (result.stream().noneMatch(resultFacade -> Facade.same(subSwitchFacade, resultFacade))) {
							result.add(subSwitchFacade);
						}
					}
				} else {
					if (result.stream().noneMatch(resultFacade -> Facade.same(facade, resultFacade))) {
						result.add(facade);
					}
				}
			}
		}
		return result;
	}

	public void setManagedInitializerFacades(List<Facade> newFacades) {
		List<Facade> oldFacades = getManagedInitializerFacades();
		List<Facade> removedFacades = oldFacades.stream()
				.filter(oldFacade -> newFacades.stream()
						.noneMatch(newFacade -> newFacade.getUnderlying() == oldFacade.getUnderlying()))
				.collect(Collectors.toList());
		for (InitializationCaseFacade caseFacade : getChildren()) {
			for (Facade facade : caseFacade.getChildren()) {
				if (facade instanceof InitializationSwitchFacade) {
					List<Facade> switchNewFacades = ((InitializationSwitchFacade) facade).getManagedInitializerFacades()
							.stream()
							.filter(switchOldFacade -> newFacades.stream().anyMatch(
									newFacade -> newFacade.getUnderlying() == switchOldFacade.getUnderlying()))
							.collect(Collectors.toList());
					((InitializationSwitchFacade) facade).setManagedInitializerFacades(switchNewFacades);
				} else {
					for (Facade removedFacade : removedFacades) {
						if (Facade.same(facade, removedFacade)) {
							facade.setConcrete(false);
						}
					}
				}
			}
		}
	}

	public List<Facade> collectLiveInitializerFacades(InstantiationContext context) {
		List<InitializationCaseFacade> children = getChildren();
		InstantiationContext childContext = new InstantiationContext(context, this);
		for (InitializationCaseFacade caseFacade : children) {
			boolean caseConditionFullfilled;
			if (caseFacade.getCondition() != null) {
				try {
					caseConditionFullfilled = InstantiationUtils.isConditionFullfilled(caseFacade.getCondition(),
							childContext);
				} catch (Exception e) {
					throw new UnexpectedError(e);
				}
			} else {
				if (children.indexOf(caseFacade) != (children.size() - 1)) {
					throw new UnexpectedError();
				}
				caseConditionFullfilled = true;
			}
			if (caseConditionFullfilled) {
				return caseFacade.collectLiveInitializerFacades(childContext);
			}
		}
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return MiscUtils.stringJoin(getManagedInitializerFacades(), ", ") + " <Switch>";
	}

}