package com.otk.jesb.instantiation;

import java.util.List;

public class RootInstanceBuilderFacade extends InstanceBuilderFacade {

	public RootInstanceBuilderFacade(RootInstanceBuilder underlying) {
		super(null, underlying);
	}

	@Override
	public RootInstanceBuilder getUnderlying() {
		return (RootInstanceBuilder) super.getUnderlying();
	}

	public static boolean isRootInitializerFacade(Facade facade) {
		return (facade instanceof ParameterInitializerFacade) && (((ParameterInitializerFacade) facade)
				.getCurrentInstanceBuilderFacade() instanceof RootInstanceBuilderFacade);
	}

	@Override
	public String getSelectedConstructorSignature() {
		List<String> options = getConstructorSignatureOptions();
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
