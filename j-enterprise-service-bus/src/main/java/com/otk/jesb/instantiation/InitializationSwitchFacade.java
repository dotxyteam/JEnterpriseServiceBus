package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		for (Map.Entry<Function, InitializationCase> caseEntry : underlying.getInitializationCaseByCondition()
				.entrySet()) {
			result.add(new InitializationCaseFacade(this, caseEntry.getKey(), caseEntry.getValue()));
		}
		result.add(new InitializationCaseFacade(this, null, underlying.getDefaultInitializationCase()));
		return result;
	}

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

	@Override
	public String toString() {
		return "<Switch>";
	}
}