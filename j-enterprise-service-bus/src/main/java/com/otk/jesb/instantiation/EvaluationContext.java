package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.otk.jesb.Variable;
import com.otk.jesb.instantiation.ListItemReplication.IterationVariable;
import com.otk.jesb.util.MiscUtils;

public class EvaluationContext {

	private List<Variable> variables = new ArrayList<Variable>();
	private Facade parentFacade;
	private Function<InstantiationFunction, InstantiationFunctionCompilationContext> instantiationFunctionCompilationContextMapper;

	public EvaluationContext(List<Variable> variables, Facade parentFacade,
			Function<InstantiationFunction, InstantiationFunctionCompilationContext> instantiationFunctionCompilationContextMapper) {
		this.variables = variables;
		this.parentFacade = parentFacade;
		this.instantiationFunctionCompilationContextMapper = instantiationFunctionCompilationContextMapper;
	}

	public EvaluationContext(EvaluationContext parentContext, Facade newParentFacade) {
		this(parentContext.getVariables(), newParentFacade,
				parentContext.getInstantiationFunctionCompilationContextMapper());
	}

	public EvaluationContext(EvaluationContext parentContext, IterationVariable newVariable) {
		this(MiscUtils.added(parentContext.getVariables(), -1, newVariable), parentContext.getParentFacade(),
				parentContext.getInstantiationFunctionCompilationContextMapper());
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public Facade getParentFacade() {
		return parentFacade;
	}

	public Function<InstantiationFunction, InstantiationFunctionCompilationContext> getInstantiationFunctionCompilationContextMapper() {
		return instantiationFunctionCompilationContextMapper;
	}

}