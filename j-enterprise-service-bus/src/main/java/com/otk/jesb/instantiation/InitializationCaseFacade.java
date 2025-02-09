package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.otk.jesb.Plan;
import com.otk.jesb.Plan.ExecutionContext;
import com.otk.jesb.Plan.ValidationContext;
import com.otk.jesb.Plan.ValidationContext.VariableDeclaration;
import com.otk.jesb.instantiation.Function.CompilationContext;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.info.field.IFieldInfo;
import xy.reflect.ui.info.method.IMethodInfo;
import xy.reflect.ui.info.parameter.IParameterInfo;
import xy.reflect.ui.info.type.DefaultTypeInfo;
import xy.reflect.ui.info.type.ITypeInfo;
import xy.reflect.ui.info.type.iterable.IListTypeInfo;

public class InitializationCaseFacade implements Facade {

	private InitializationSwitchFacade parent;
	private Function condition;
	private InitializationCase underlying;

	public InitializationCaseFacade(InitializationSwitchFacade parent, Function condition,
			InitializationCase underlying) {
		this.parent = parent;
		this.condition = condition;
		this.underlying = underlying;
	}

	public Function getCondition() {
		return condition;
	}

	@Override
	public List<Facade> getChildren() {
		List<Facade> result = new ArrayList<Facade>();
		InstanceBuilderFacade instanceBuilderFacade = getCurrentInstanceBuilderFacade();
		ITypeInfo typeInfo = instanceBuilderFacade.getTypeInfo();
		IMethodInfo constructor = MiscUtils.getConstructorInfo(typeInfo,
				instanceBuilderFacade.getSelectedConstructorSignature());
		if (constructor != null) {
			for (IParameterInfo parameterInfo : constructor.getParameters()) {
				if (isParameterInitializedInChildSwitch(parameterInfo)) {
					continue;
				}
				if (!mustHaveParameterFacadeLocally(parameterInfo)) {
					continue;
				}
				result.add(createParameterInitializerFacade(parameterInfo.getPosition()));
			}
		}
		if (typeInfo instanceof IListTypeInfo) {
			if (mustHaveListItemFacadesLocally()) {
				int i = 0;
				for (; i < underlying.getListItemInitializers().size();) {
					result.add(createListItemInitializerFacade(i));
					i++;
				}
				result.add(createListItemInitializerFacade(i));
			}
		} else {
			for (IFieldInfo fieldInfo : typeInfo.getFields()) {
				if (fieldInfo.isGetOnly()) {
					continue;
				}
				if (isFieldInitializedInChildSwitch(fieldInfo)) {
					continue;
				}
				if (!mustHaveFieldFacadeLocally(fieldInfo)) {
					continue;
				}
				result.add(createFieldInitializerFacade(fieldInfo.getName()));
			}
		}
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			result.add(createInitializationSwitchFacade(initializationSwitch));
		}
		Collections.sort(result, new Comparator<Facade>() {
			List<Class<?>> CLASSES_ORDER = Arrays.asList(ParameterInitializerFacade.class, FieldInitializerFacade.class,
					ListItemInitializerFacade.class, InitializationSwitchFacade.class);

			@Override
			public int compare(Facade o1, Facade o2) {
				if (!o1.getClass().equals(o2.getClass())) {
					return Integer.valueOf(CLASSES_ORDER.indexOf(o1.getClass()))
							.compareTo(Integer.valueOf(CLASSES_ORDER.indexOf(o2.getClass())));
				}
				if ((o1 instanceof ParameterInitializerFacade) && (o2 instanceof ParameterInitializerFacade)) {
					ParameterInitializerFacade pif1 = (ParameterInitializerFacade) o1;
					ParameterInitializerFacade pif2 = (ParameterInitializerFacade) o2;
					return Integer.valueOf(pif1.getParameterPosition())
							.compareTo(Integer.valueOf(pif2.getParameterPosition()));
				} else if ((o1 instanceof FieldInitializerFacade) && (o2 instanceof FieldInitializerFacade)) {
					FieldInitializerFacade fif1 = (FieldInitializerFacade) o1;
					FieldInitializerFacade fif2 = (FieldInitializerFacade) o2;
					return fif1.getFieldInfo().getName().compareTo(fif2.getFieldInfo().getName());
				} else if ((o1 instanceof ListItemInitializerFacade) && (o2 instanceof ListItemInitializerFacade)) {
					ListItemInitializerFacade liif1 = (ListItemInitializerFacade) o1;
					ListItemInitializerFacade liif2 = (ListItemInitializerFacade) o2;
					return Integer.valueOf(liif1.getIndex()).compareTo(Integer.valueOf(liif2.getIndex()));
				} else if ((o1 instanceof InitializationSwitchFacade) && (o2 instanceof InitializationSwitchFacade)) {
					// InitializationSwitchFacade isf1 = (InitializationSwitchFacade) o1;
					// InitializationSwitchFacade isf2 = (InitializationSwitchFacade) o2;
					return 0;
				} else {
					throw new AssertionError();
				}

			}
		});
		return result;
	}

	protected InitializationSwitchFacade createInitializationSwitchFacade(InitializationSwitch initializationSwitch) {
		return new InitializationSwitchFacade(this, initializationSwitch);
	}

	protected FieldInitializerFacade createFieldInitializerFacade(String fieldName) {
		return new FieldInitializerFacade(this, fieldName);
	}

	protected ListItemInitializerFacade createListItemInitializerFacade(int index) {
		return new ListItemInitializerFacade(this, index);
	}

	protected ParameterInitializerFacade createParameterInitializerFacade(int parameterPosition) {
		return new ParameterInitializerFacade(this, parameterPosition);
	}

	protected boolean isFieldInitializedInChildSwitch(IFieldInfo fieldInfo) {
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(switchFacade, null,
					switchFacade.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.getUnderlying().getFieldInitializer(fieldInfo.getName()) != null) {
				return true;
			}
			if (defaultCaseFacade.isFieldInitializedInChildSwitch(fieldInfo)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isParameterInitializedInChildSwitch(IParameterInfo parameterInfo) {
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(switchFacade, null,
					switchFacade.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.getUnderlying().getParameterInitializer(parameterInfo.getPosition(),
					parameterInfo.getType().getName()) != null) {
				return true;
			}
			if (defaultCaseFacade.isParameterInitializedInChildSwitch(parameterInfo)) {
				return true;
			}
		}
		return false;
	}

	protected boolean areListItemsInitializedInChildSwitch() {
		for (InitializationSwitch initializationSwitch : underlying.getInitializationSwitches()) {
			InitializationSwitchFacade switchFacade = new InitializationSwitchFacade(this, initializationSwitch);
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(switchFacade, null,
					switchFacade.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.getUnderlying().getListItemInitializers().size() > 0) {
				return true;
			}
			if (defaultCaseFacade.areListItemsInitializedInChildSwitch()) {
				return true;
			}
		}
		return false;
	}

	protected boolean mustHaveParameterFacadeLocally(IParameterInfo parameterInfo) {
		if (isParameterInitializedInChildSwitch(parameterInfo)) {
			return false;
		}
		if (isDefaultCaseFacade()) {
			if (underlying.getParameterInitializer(parameterInfo.getPosition(),
					parameterInfo.getType().getName()) != null) {
				return true;
			}
		} else {
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(parent, null,
					parent.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.mustHaveParameterFacadeLocally(parameterInfo)) {
				return true;
			}
			if (defaultCaseFacade.isParameterInitializedInChildSwitch(parameterInfo)) {
				return true;
			}
		}
		return false;
	}

	protected boolean mustHaveFieldFacadeLocally(IFieldInfo fieldInfo) {
		if (isFieldInitializedInChildSwitch(fieldInfo)) {
			return false;
		}
		if (isDefaultCaseFacade()) {
			if (underlying.getFieldInitializer(fieldInfo.getName()) != null) {
				return true;
			}
		} else {
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(parent, null,
					parent.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.mustHaveFieldFacadeLocally(fieldInfo)) {
				return true;
			}
			if (defaultCaseFacade.isFieldInitializedInChildSwitch(fieldInfo)) {
				return true;
			}
		}
		return false;
	}

	protected boolean mustHaveListItemFacadesLocally() {
		if (isDefaultCaseFacade()) {
			if (underlying.getListItemInitializers().size() > 0) {
				return true;
			}
			if (areListItemsInitializedInChildSwitch()) {
				return true;
			}
		} else {
			InitializationCaseFacade defaultCaseFacade = new InitializationCaseFacade(parent, null,
					parent.getUnderlying().getDefaultInitializationCase());
			if (defaultCaseFacade.mustHaveListItemFacadesLocally()) {
				return true;
			}
			if (defaultCaseFacade.areListItemsInitializedInChildSwitch()) {
				return true;
			}
		}
		return false;
	}

	public InstanceBuilderFacade getCurrentInstanceBuilderFacade() {
		return (InstanceBuilderFacade) Facade.getAncestors(this).stream()
				.filter(f -> (f instanceof InstanceBuilderFacade)).findFirst().get();
	}

	protected boolean isDefaultCaseFacade() {
		return condition == null;
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
	public InitializationCase getUnderlying() {
		return underlying;
	}

	@Override
	public InitializationSwitchFacade getParent() {
		return parent;
	}

	public List<Facade> collectInitializerFacades(EvaluationContext context) {
		List<Facade> result = new ArrayList<Facade>();
		for (Facade facade : getChildren()) {
			if (facade instanceof ParameterInitializerFacade) {
				result.add(facade);
			} else if (facade instanceof FieldInitializerFacade) {
				result.add(facade);
			} else if (facade instanceof ListItemInitializerFacade) {
				result.add(facade);
			} else if (facade instanceof InitializationSwitchFacade) {
				result.addAll(((InitializationSwitchFacade) facade)
						.collectInitializerFacades(createEvaluationContextForChildren(context.getExecutionContext())));
			}
		}
		return result;
	}

	protected EvaluationContext createEvaluationContextForChildren(ExecutionContext executionContext) {
		return new EvaluationContext(executionContext, this);
	}

	public CompilationContext findFunctionCompilationContext(Function function, ValidationContext validationContext) {
		for (Facade facade : getChildren()) {
			if (facade instanceof ParameterInitializerFacade) {
				ParameterInitializerFacade currentFacade = (ParameterInitializerFacade) facade;
				if (currentFacade.getParameterValue() == function) {
					return new CompilationContext(new VerificationContext(validationContext, currentFacade),
							((DefaultTypeInfo) currentFacade.getParameterInfo().getType()).getJavaType());
				}
				if (currentFacade.getParameterValue() instanceof InstanceBuilder) {
					CompilationContext compilationContext = new InstanceBuilderFacade(currentFacade,
							(InstanceBuilder) currentFacade.getParameterValue())
									.findFunctionCompilationContext(function, validationContext);
					if (compilationContext != null) {
						return compilationContext;
					}
				}
			} else if (facade instanceof FieldInitializerFacade) {
				FieldInitializerFacade currentFacade = (FieldInitializerFacade) facade;
				if (!currentFacade.isConcrete()) {
					continue;
				}
				if (currentFacade.getCondition() == function) {
					return new CompilationContext(new VerificationContext(validationContext, currentFacade),
							boolean.class);
				}
				if (currentFacade.getFieldValue() == function) {
					return new CompilationContext(new VerificationContext(validationContext, currentFacade),
							((DefaultTypeInfo) currentFacade.getFieldInfo().getType()).getJavaType());
				}
				if (currentFacade.getFieldValue() instanceof InstanceBuilder) {
					CompilationContext compilationContext = new InstanceBuilderFacade(currentFacade,
							(InstanceBuilder) currentFacade.getFieldValue()).findFunctionCompilationContext(function,
									validationContext);
					if (compilationContext != null) {
						return compilationContext;
					}
				}
			} else if (facade instanceof ListItemInitializerFacade) {
				ListItemInitializerFacade currentFacade = (ListItemInitializerFacade) facade;
				if (!currentFacade.isConcrete()) {
					continue;
				}
				if (currentFacade.getCondition() == function) {
					return new CompilationContext(new VerificationContext(validationContext, currentFacade),
							boolean.class);
				}
				VariableDeclaration iterationVariableDeclaration = null;
				int iterationVariableDeclarationPosition = -1;
				if (currentFacade.getItemReplicationFacade() != null) {
					if (currentFacade.getItemReplicationFacade().getIterationListValue() == function) {
						return new CompilationContext(new VerificationContext(validationContext, currentFacade),
								Object.class);
					}
					if (currentFacade.getItemReplicationFacade().getIterationListValue() instanceof InstanceBuilder) {
						CompilationContext compilationContext = new InstanceBuilderFacade(currentFacade,
								(InstanceBuilder) currentFacade.getItemReplicationFacade().getIterationListValue())
										.findFunctionCompilationContext(function, validationContext);
						if (compilationContext != null) {
							return compilationContext;
						}
					}
					iterationVariableDeclaration = new Plan.ValidationContext.VariableDeclaration() {

						@Override
						public String getVariableName() {
							return currentFacade.getItemReplicationFacade().getIterationVariableName();
						}

						@Override
						public Class<?> getVariableClass() {
							return Object.class;
						}
					};
					iterationVariableDeclarationPosition = validationContext.getVariableDeclarations().size();
				}
				if (currentFacade.getItemValue() == function) {
					ValidationContext iterationValidationContext = validationContext;
					if (iterationVariableDeclaration != null) {
						List<VariableDeclaration> newVariableDeclarations = new ArrayList<Plan.ValidationContext.VariableDeclaration>(
								validationContext.getVariableDeclarations());
						newVariableDeclarations.add(iterationVariableDeclarationPosition, iterationVariableDeclaration);
						iterationValidationContext = new ValidationContext(iterationValidationContext.getPlan(),
								newVariableDeclarations);
					}
					return new CompilationContext(new VerificationContext(iterationValidationContext, currentFacade),
							((DefaultTypeInfo) currentFacade.getItemType()).getJavaType());
				}
				if (currentFacade.getItemValue() instanceof InstanceBuilder) {
					ValidationContext iterationValidationContext = validationContext;
					if (iterationVariableDeclaration != null) {
						List<VariableDeclaration> newVariableDeclarations = new ArrayList<Plan.ValidationContext.VariableDeclaration>(
								validationContext.getVariableDeclarations());
						newVariableDeclarations.add(iterationVariableDeclarationPosition, iterationVariableDeclaration);
						iterationValidationContext = new ValidationContext(iterationValidationContext.getPlan(),
								newVariableDeclarations);
					}
					CompilationContext compilationContext = new InstanceBuilderFacade(currentFacade,
							(InstanceBuilder) currentFacade.getItemValue()).findFunctionCompilationContext(function,
									iterationValidationContext);
					if (compilationContext != null) {
						return compilationContext;
					}
				}
			} else if (facade instanceof InitializationSwitchFacade) {
				InitializationSwitchFacade currentFacade = (InitializationSwitchFacade) facade;
				for (Facade childFacade : currentFacade.getChildren()) {
					InitializationCaseFacade caseFacade = (InitializationCaseFacade) childFacade;
					if (caseFacade.getCondition() == function) {
						return new CompilationContext(new VerificationContext(validationContext, currentFacade),
								boolean.class);
					}
					CompilationContext compilationContext = caseFacade.findFunctionCompilationContext(function,
							validationContext);
					if (compilationContext != null) {
						return compilationContext;
					}
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		if (isDefaultCaseFacade()) {
			return "<Default>";
		} else {
			return "<Case "
					+ new ArrayList<InitializationCase>(
							parent.getUnderlying().getInitializationCaseByCondition().values()).indexOf(underlying)
					+ ">";
		}
	}
}