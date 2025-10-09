package com.otk.jesb.operation;

import com.otk.jesb.solution.Plan;
import com.otk.jesb.solution.Step;
import com.otk.jesb.solution.Plan.ExecutionContext;
import com.otk.jesb.solution.Plan.ExecutionInspector;
import com.otk.jesb.ValidationError;

/**
 * This type was extracted from {@link OperationBuilder} to allow the convenient
 * distribution (split) of its sub-types information into structured member
 * types.
 * 
 * @author olitank
 *
 * @param <T> The specific associated {@link Operation} or structured member
 *            type.
 */
public interface OperationStructureBuilder<T> {

	/**
	 * @param context            The current {@link Plan} execution context.
	 * @param executionInspector The current execution inspector.
	 * @return An {@link Operation} instance or member constructed and configured
	 *         according the provided context.
	 * @throws Exception If an error occurs.
	 */
	T build(ExecutionContext context, ExecutionInspector executionInspector) throws Exception;

	/**
	 * @param recursively Whether the validation is recursively executed on
	 *                    sub-objects or not.
	 * @param plan        The current {@link Plan}.
	 * @param step        The current {@link Step}.
	 * @throws ValidationError If the current object is considered as invalid.
	 */
	void validate(boolean recursively, Plan plan, Step step) throws ValidationError;

}
