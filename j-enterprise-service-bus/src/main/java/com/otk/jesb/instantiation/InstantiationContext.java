package com.otk.jesb.instantiation;

import java.util.ArrayList;
import java.util.List;
import com.otk.jesb.Variable;
import com.otk.jesb.VariableDeclaration;
import com.otk.jesb.instantiation.ListItemReplication.IterationVariable;
import com.otk.jesb.util.MiscUtils;

/**
 * Contains contextual information to use during the execution of
 * {@link InstanceBuilder#build(InstantiationContext)}.
 * 
 * @author olitank
 *
 */
public class InstantiationContext {

	private List<Variable> variables = new ArrayList<Variable>();
	private Facade parentFacade;
	private List<VariableDeclaration> variableDeclarations;

	protected InstantiationContext(Facade parentFacade, List<Variable> variables,
			List<VariableDeclaration> variableDeclarations) {
		this.parentFacade = parentFacade;
		this.variables = variables;
		this.variableDeclarations = variableDeclarations;
	}

	protected InstantiationContext(InstantiationContext parentContext, Facade newParentFacade) {
		this(newParentFacade, parentContext.getVariables(), parentContext.getVariableDeclarations());
	}

	protected InstantiationContext(InstantiationContext parentContext, IterationVariable newVariable) {
		this(parentContext.getParentFacade(), MiscUtils.added(parentContext.getVariables(), -1, newVariable),
				parentContext.getVariableDeclarations());
	}

	public InstantiationContext(List<Variable> variables, List<VariableDeclaration> variableDeclarations) {
		this(null, variables, variableDeclarations);
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public Facade getParentFacade() {
		return parentFacade;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public InstantiationFunctionCompilationContext getFunctionCompilationContex(InstantiationFunction function) {
		return new InstantiationFunctionCompilationContext(variableDeclarations, parentFacade);
	}

}