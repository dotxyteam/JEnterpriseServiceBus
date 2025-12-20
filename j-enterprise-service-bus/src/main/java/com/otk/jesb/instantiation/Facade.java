package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.ValidationError;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.TreeVisitor;

/**
 * This is the base of the wrapper classes used to improve the usability of
 * instantiation structures/nodes ({@link InstanceBuilder},
 * {@link FieldInitializer}, {@link InitializationSwitch}, ...).
 * 
 * Note that these wrapper classes use reflection to provide validation,
 * preview, etc.
 * 
 * @author olitank
 *
 */
public abstract class Facade {

	protected Solution solutionInstance;

	public Facade(Solution solutionInstance) {
		if(solutionInstance == null) {
			throw new UnexpectedError();
		}
		this.solutionInstance = solutionInstance;
	}

	/**
	 * @return A text summarizing the specific behavior of the underlying
	 *         instantiation structure/node.
	 */
	public abstract String express();

	/**
	 * @return The parent {@link Facade}.
	 */
	public abstract Facade getParent();

	/**
	 * @return The list of valid child {@link Facade} instances.
	 */
	public abstract List<? extends Facade> getChildren();

	/**
	 * @return Whether the underlying instantiation structure/node is connected to
	 *         the root and therefore actually active. Otherwise, the current
	 *         {@link Facade} is just a glimpse of the possibilities.
	 */
	public abstract boolean isConcrete();

	/**
	 * Updates whether the underlying instantiation structure/node is connected to
	 * the root and therefore actually active. Otherwise, the current {@link Facade}
	 * is just a glimpse of the possibilities.
	 * 
	 * @param b The new status.
	 */
	public abstract void setConcrete(boolean b);

	/**
	 * @return The underlying instantiation structure/node.
	 */
	public abstract Object getUnderlying();

	/**
	 * @param function                 This current {@link Facade} function whose
	 *                                 evaluation context will be associated with
	 *                                 the result of this method. If null is
	 *                                 provided then the result of this method is
	 *                                 not calculated for a function of the current
	 *                                 {@link Facade}, but rather for the function
	 *                                 of a descendant {@link Facade}.
	 * @param baseVariableDeclarations The {@link VariableDeclaration} instances of
	 *                                 the root evaluation context.
	 * @return The additional {@link VariableDeclaration} instances generated (in
	 *         addition to those in the the root evaluation context) by the
	 *         ascending branch of the current instantiation {@link Facade}. These
	 *         will be part of the evaluation context of the provided function (if
	 *         non-null) or a descendant {@link Facade} function.
	 */
	public abstract List<VariableDeclaration> getAdditionalVariableDeclarations(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations);

	/**
	 * @param function                 A function of the current {@link Facade}.
	 * @param baseVariableDeclarations The {@link VariableDeclaration} instances of
	 *                                 the root evaluation context.
	 * @return The return type associated with provided function.
	 */
	public abstract Class<?> getFunctionReturnType(InstantiationFunction function,
			List<VariableDeclaration> baseVariableDeclarations);

	/**
	 * @param recursively          Whether the validation is recursively executed on
	 *                             sub-objects or not.
	 * @param variableDeclarations The {@link VariableDeclaration} instances of the
	 *                             root evaluation context.
	 * @throws ValidationError If the current object is considered as invalid.
	 */
	public abstract void validate(boolean recursively, List<VariableDeclaration> variableDeclarations)
			throws ValidationError;

	/**
	 * @return The current solution.
	 */
	public Solution getSolutionInstance() {
		return solutionInstance;
	}

	/**
	 * @return Whether the current {@link Facade} may be validated or not.
	 */
	public boolean isValidable() {
		return isConcrete();
	}

	/**
	 * @param visitor The visitor instance.
	 * @return The status resulting from the current visit.
	 */
	public TreeVisitor.VisitStatus visit(TreeVisitor<Facade> visitor) {
		return TreeVisitor.visitTreeFrom(this, visitor, Facade::getChildren);
	}

	/**
	 * @param facade The current {@link Facade}.
	 * @return The list of ancestor {@link Facade} instances.
	 */
	public static List<Facade> getAncestors(Facade facade) {
		List<Facade> result = new ArrayList<Facade>();
		Facade parentFacade;
		while ((parentFacade = facade.getParent()) != null) {
			result.add(parentFacade);
			facade = parentFacade;
		}
		return result;
	}

	/**
	 * @param facade The current facade.
	 * @return The root facade.
	 */
	public static Facade getRoot(Facade facade) {
		List<Facade> ancestors = getAncestors(facade);
		if (ancestors.size() == 0) {
			return facade;
		}
		return ancestors.get(ancestors.size() - 1);
	}

	/**
	 * @param node         The underlying instantiation structure/node.
	 * @param parentFacade The parent facade.
	 * @return The appropriate facade for the provided underlying instantiation
	 *         structure.
	 */
	public static Facade get(Object node, Facade parentFacade, Solution solutionInstance) {
		if (node instanceof RootInstanceBuilder) {
			if (parentFacade != null) {
				throw new UnexpectedError();
			}
			return new RootInstanceBuilderFacade((RootInstanceBuilder) node, solutionInstance);
		} else if (node instanceof MapEntryBuilder) {
			return new MapEntryBuilderFacade(parentFacade, (MapEntryBuilder) node, solutionInstance);
		} else if (node instanceof InstanceBuilder) {
			return new InstanceBuilderFacade(parentFacade, (InstanceBuilder) node, solutionInstance);
		} else if (node instanceof FieldInitializer) {
			return new FieldInitializerFacade(parentFacade, ((FieldInitializer) node).getFieldName(), solutionInstance);
		} else if (node instanceof ParameterInitializer) {
			return new ParameterInitializerFacade(parentFacade, ((ParameterInitializer) node).getParameterPosition(),
					solutionInstance);
		} else if (node instanceof ListItemInitializer) {
			return new ListItemInitializerFacade(parentFacade, ((ListItemInitializer) node).getIndex(),
					solutionInstance);
		} else if (node instanceof InitializationSwitch) {
			return new InitializationSwitchFacade(parentFacade, (InitializationSwitch) node, solutionInstance);
		} else if (node instanceof InitializationCase) {
			return new InitializationCaseFacade(
					(InitializationSwitchFacade) parentFacade, ((InitializationSwitchFacade) parentFacade)
							.getUnderlying().findCondition((InitializationCase) node),
					(InitializationCase) node, solutionInstance);
		} else {
			throw new UnexpectedError();
		}
	}

	/**
	 * @param facade1 1st facade to compare.
	 * @param facade2 2nd facade to compare.
	 * @return whether the provided facades are equivalent (same kind and same
	 *         identifier).
	 */
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