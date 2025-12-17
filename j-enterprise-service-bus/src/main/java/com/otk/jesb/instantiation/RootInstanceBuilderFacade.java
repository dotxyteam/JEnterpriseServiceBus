package com.otk.jesb.instantiation;

import java.util.List;

import com.otk.jesb.solution.Solution;

public class RootInstanceBuilderFacade extends InstanceBuilderFacade {

	public RootInstanceBuilderFacade(RootInstanceBuilder underlying, Solution solutionInstance) {
		super(null, underlying, solutionInstance);
	}

	@Override
	public RootInstanceBuilder getUnderlying() {
		return (RootInstanceBuilder) super.getUnderlying();
	}

	@Override
	public String getSelectedConstructorSignature() {
		List<String> options = getConstructorSignatureOptions(solutionInstance);
		if (options.size() > 0) {
			return options.get(0);
		}
		return null;
	}

	@Override
	public void setSelectedConstructorSignature(String selectedConstructorSignature) {
		throw new UnsupportedOperationException();
	}
}
