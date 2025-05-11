package com.otk.jesb.instantiation;

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

}
