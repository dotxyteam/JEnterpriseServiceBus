package com.otk.jesb.activation.builtin;

import com.otk.jesb.ValidationError;
import com.otk.jesb.compiler.CompilationError;
import com.otk.jesb.operation.builtin.ExecutePlan;
import com.otk.jesb.PotentialError;
import com.otk.jesb.Structure.ClassicStructure;
import com.otk.jesb.activation.ActivationHandler;
import com.otk.jesb.activation.Activator;
import com.otk.jesb.activation.ActivatorMetadata;
import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Solution;
import com.otk.jesb.util.InstantiationUtils;
import com.otk.jesb.util.MiscUtils;
import com.otk.jesb.util.UpToDate;
import com.otk.jesb.util.UpToDate.VersionAccessException;

import xy.reflect.ui.info.ResourcePath;

public class Operate extends Activator {

	private ClassicStructure inputStructure;
	private ClassicStructure outputStructure;

	private UpToDate<Solution, Class<?>> upToDateInputClass = new UpToDateInputClass();
	private UpToDate<Solution, Class<?>> upToDateOutputClass = new UpToDateOutputClass();

	public ClassicStructure getInputStructure() {
		return inputStructure;
	}

	public void setInputStructure(ClassicStructure inputStructure) {
		this.inputStructure = inputStructure;
	}

	public ClassicStructure getOutputStructure() {
		return outputStructure;
	}

	public void setOutputStructure(ClassicStructure outputStructure) {
		this.outputStructure = outputStructure;
	}

	@Override
	public Class<?> getInputClass(Solution solutionInstance) {
		try {
			return upToDateInputClass.get(solutionInstance);
		} catch (VersionAccessException e) {
			throw new PotentialError(e);
		}
	}

	@Override
	public Class<?> getOutputClass(Solution solutionInstance) {
		try {
			return upToDateOutputClass.get(solutionInstance);
		} catch (VersionAccessException e) {
			throw new PotentialError(e);
		}
	}

	@Override
	public boolean isAutomaticallyTriggerable() {
		return false;
	}

	@Override
	public void initializeAutomaticTrigger(ActivationHandler activationHandler, Solution solutionInstance) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void finalizeAutomaticTrigger(Solution solutionInstance) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAutomaticTriggerReady() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void validate(boolean recursively, Solution solutionInstance, Plan plan) throws ValidationError {
		super.validate(recursively, solutionInstance, plan);
		if (inputStructure != null) {
			try {
				inputStructure.validate(recursively, solutionInstance);
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate the input structure", e);
			}
		}
		if (outputStructure != null) {
			try {
				outputStructure.validate(recursively, solutionInstance);
			} catch (ValidationError e) {
				throw new ValidationError("Failed to validate the output structure", e);
			}
		}
	}

	private class UpToDateInputClass extends UpToDate<Solution, Class<?>> {
		@Override
		protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
			return (inputStructure != null)
					? MiscUtils.serialize(inputStructure, solutionInstance.getRuntime().getXstream())
					: null;
		}

		@Override
		protected Class<?> obtainLatest(Solution solutionInstance, Object versionIdentifier) {
			if (inputStructure == null) {
				return null;
			} else {
				try {
					String className = Plan.class.getPackage().getName() + "." + Plan.class.getSimpleName() + "Input"
							+ InstantiationUtils
									.toRelativeTypeNameVariablePart(MiscUtils.toDigitalUniqueIdentifier(Operate.this));
					return solutionInstance.getRuntime().getInMemoryCompiler().compile(className,
							inputStructure.generateJavaTypeSourceCode(className, solutionInstance));
				} catch (CompilationError e) {
					throw new PotentialError(e);
				}
			}
		}
	}

	private class UpToDateOutputClass extends UpToDate<Solution, Class<?>> {
		@Override
		protected Object retrieveLastVersionIdentifier(Solution solutionInstance) {
			return (outputStructure != null)
					? MiscUtils.serialize(outputStructure, solutionInstance.getRuntime().getXstream())
					: null;
		}

		@Override
		protected Class<?> obtainLatest(Solution solutionInstance, Object versionIdentifier) {
			if (outputStructure == null) {
				return null;
			} else {
				try {
					String className = Plan.class.getPackage().getName() + "." + Plan.class.getSimpleName() + "Output"
							+ InstantiationUtils
									.toRelativeTypeNameVariablePart(MiscUtils.toDigitalUniqueIdentifier(Operate.this));
					return solutionInstance.getRuntime().getInMemoryCompiler().compile(className,
							outputStructure.generateJavaTypeSourceCode(className, solutionInstance));
				} catch (CompilationError e) {
					throw new PotentialError(e);
				}
			}
		}
	}

	public static class Metadata implements ActivatorMetadata {

		@Override
		public ResourcePath getActivatorIconImagePath() {
			return new ResourcePath(
					ResourcePath.specifyClassPathResourceLocation(Operate.class.getName().replace(".", "/") + ".png"));
		}

		@Override
		public Class<? extends Activator> getActivatorClass() {
			return Operate.class;
		}

		@Override
		public String getActivatorName() {
			return "(Sub-Plan) Handle <" + new ExecutePlan.Metadata().getOperationTypeName() + "> Operation";
		}

	}
}
