package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.otk.jesb.util.MiscUtils;

public class InitializationSwitchFacade implements Facade {

	private Facade parent;
	private InitializationSwitch underlying;

	public InitializationSwitchFacade(Facade parent, InitializationSwitch underlying) {
		this.parent = parent;
		this.underlying = underlying;
	}

	public InitializationSwitchFacade(Facade parent, int caseCount, List<Facade> initializerFacades) {
		if (caseCount < 1) {
			throw new IllegalArgumentException("Invalid number of cases: " + caseCount + "(must not be < 1)");
		}
		this.parent = parent;
		underlying = new InitializationSwitch();
		{
			for (int iCase = 0; iCase < caseCount; iCase++) {
				InitializationCase newCase = new InitializationCase();
				underlying.getInitializationCaseByCondition().put(new Function("return false;"), newCase);
			}
			InitializationCase defaultCase = new InitializationCase();
			{
				for (Facade initializerFacade : initializerFacades) {
					if (initializerFacade instanceof ParameterInitializerFacade) {
						ParameterInitializer initializer = ((ParameterInitializerFacade) initializerFacade)
								.getUnderlying();
						if (!((InitializationCase) parent.getUnderlying()).getParameterInitializers()
								.remove(initializer)) {
							throw new AssertionError();
						}
						defaultCase.getParameterInitializers().add(initializer);
					} else if (initializerFacade instanceof FieldInitializerFacade) {
						((FieldInitializerFacade) initializerFacade).setConcrete(true);
						FieldInitializer initializer = ((FieldInitializerFacade) initializerFacade).getUnderlying();
						if (!((InitializationCase) parent.getUnderlying()).getFieldInitializers().remove(initializer)) {
							throw new AssertionError();
						}
						defaultCase.getFieldInitializers().add(initializer);
					} else if (initializerFacade instanceof ListItemInitializerFacade) {
						((ListItemInitializerFacade) initializerFacade).setConcrete(true);
						ListItemInitializer initializer = ((ListItemInitializerFacade) initializerFacade)
								.getUnderlying();
						if (!((InitializationCase) parent.getUnderlying()).getListItemInitializers()
								.remove(initializer)) {
							throw new AssertionError();
						}
						defaultCase.getListItemInitializers().add(initializer);
					} else {
						throw new AssertionError();
					}
				}
				underlying.setDefaultInitializationCase(defaultCase);
			}
			((InitializationCase) parent.getUnderlying()).getInitializationSwitches().add(underlying);
		}
		setConcrete(true);
	}

	@Override
	public List<InitializationCaseFacade> getChildren() {
		List<InitializationCaseFacade> result = new ArrayList<InitializationCaseFacade>();
		for (Map.Entry<Function, InitializationCase> caseEntry : underlying.getInitializationCaseByCondition()
				.entrySet()) {
			result.add(new InitializationCaseFacade(this, caseEntry.getKey(), caseEntry.getValue()));
		}
		result.add(new InitializationCaseFacade(this, null, underlying.getDefaultInitializationCase()));
		return result;
	}

	public void setChildren(List<InitializationCaseFacade> newChildren) {
		if (newChildren.stream().noneMatch(caseFacade -> caseFacade.getCondition() == null)) {
			throw new UnsupportedOperationException("Cannot remove the default case");
		}
		underlying.getInitializationCaseByCondition().clear();
		for (InitializationCaseFacade caseFacade : newChildren) {
			if (caseFacade.getCondition() == null) {
				continue;
			}
			underlying.getInitializationCaseByCondition().put(caseFacade.getCondition(), caseFacade.getUnderlying());
		}
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
		return true;
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
		}
	}

	@Override
	public InitializationSwitch getUnderlying() {
		return underlying;
	}

	public List<Facade> listManagedInitializerFacades() {
		List<Facade> result = new ArrayList<Facade>();
		InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(this, null,
				underlying.getDefaultInitializationCase());
		for (Facade facade : defaultCaseFacade.getChildren()) {
			if (facade instanceof InitializationSwitchFacade) {
				result.addAll(((InitializationSwitchFacade) facade).listManagedInitializerFacades());
			} else {
				result.add(facade);
			}
		}
		return result;
	}

	public List<Facade> collectInitializerFacades(EvaluationContext context) {
		List<InitializationCaseFacade> children = getChildren();
		EvaluationContext childContext = new EvaluationContext(context.getExecutionContext(), this);
		for (InitializationCaseFacade caseFacade : children) {
			boolean caseConditionFullfilled;
			if (caseFacade.getCondition() != null) {
				try {
					caseConditionFullfilled = MiscUtils.isConditionFullfilled(caseFacade.getCondition(), childContext);
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			} else {
				if (children.indexOf(caseFacade) != (children.size() - 1)) {
					throw new AssertionError();
				}
				caseConditionFullfilled = true;
			}
			if (caseConditionFullfilled) {
				return caseFacade.collectInitializerFacades(childContext);
			}
		}
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return MiscUtils.stringJoin(listManagedInitializerFacades(), ", ") + " <Switch>";
	}

}