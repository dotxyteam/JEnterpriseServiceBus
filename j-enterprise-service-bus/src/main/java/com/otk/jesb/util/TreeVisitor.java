package com.otk.jesb.util;

import java.util.function.Function;

/**
 * Simple generic tree visitor interface.
 * 
 * @author olitank
 *
 * @param <T> The type of the visited elements.
 */
public interface TreeVisitor<T> {

	/**
	 * Called for each visited element.
	 * 
	 * @param t The currently visited element.
	 * @return the visit status.
	 */
	VisitStatus visitNode(T t);

	/**
	 * Enumeration allowing to express how the visit continuation should be managed.
	 * 
	 * @author olitank
	 *
	 */
	public enum VisitStatus {

		/**
		 * The visit was not or should not be interrupted.
		 */
		VISIT_NOT_INTERRUPTED,

		/**
		 * The subtree visit was or should be stopped, but not the whole tree visit.
		 */
		SUBTREE_VISIT_INTERRUPTED,

		/**
		 * The whole tree visit was or should be stopped.
		 */
		TREE_VISIT_INTERRUPTED
	}

	/**
	 * Performs a subtree visit by using a "depth-first" algorithm.
	 * 
	 * @param <T>              The type of the visited elements.
	 * @param t                The base node of the visit.
	 * @param visitor          The visitor.
	 * @param childrenAccessor A function that retrieves the children of the given
	 *                         node.
	 * @return the final visit status. Note that if at least 1 descendant subtree
	 *         visit was interrupted, then the final status will necessarily be
	 *         {@link VisitStatus#SUBTREE_VISIT_INTERRUPTED} or
	 *         {@link VisitStatus#TREE_VISIT_INTERRUPTED}.
	 */
	public static <T> VisitStatus visitTreeFrom(T t, TreeVisitor<T> visitor,
			Function<T, Iterable<? extends T>> childrenAccessor) {

		TreeVisitor.VisitStatus finalItemVisitStatus = visitor.visitNode(t);
		if (finalItemVisitStatus == VisitStatus.TREE_VISIT_INTERRUPTED) {
			return VisitStatus.TREE_VISIT_INTERRUPTED;
		}
		return visitTreesFrom(childrenAccessor.apply(t), child -> visitTreeFrom(child, visitor, childrenAccessor),
				finalItemVisitStatus);
	}

	/**
	 * Allows you to recursively visit the provided nodes. The visit can be
	 * interrupted depending on the successive statuses of individual visits.
	 *
	 * @param <T>              The type of the visited elements.
	 * @param ts               The nodes to visit.
	 * @param recursiveVisitor A function performing a recursive visit.
	 * @param initialStatus    The initial status of the visit on which the final
	 *                         status of the visit depends.
	 * @return A final combination of the initial status of the visit and the
	 *         successive statuses of the node visits.
	 */
	public static <T> VisitStatus visitTreesFrom(Iterable<? extends T> ts, Function<T, VisitStatus> recursiveVisitor,
			VisitStatus initialStatus) {
		VisitStatus result = initialStatus;
		if (result != VisitStatus.SUBTREE_VISIT_INTERRUPTED) {
			for (T child : ts) {
				VisitStatus childItemVisitStatus = recursiveVisitor.apply(child);
				if (childItemVisitStatus == VisitStatus.TREE_VISIT_INTERRUPTED) {
					return VisitStatus.TREE_VISIT_INTERRUPTED;
				}
				result = combine(result, childItemVisitStatus);
			}
		}
		return result;
	}

	/**
	 * @param parentStatus The initial visit status.
	 * @param childStatus  Another visit status.
	 * @return A combination of the given visit statues. The result cannot be "less
	 *         interrupted" than the initial parent status.
	 */
	public static VisitStatus combine(VisitStatus parentStatus, VisitStatus childStatus) {
		VisitStatus result = parentStatus;
		if (childStatus == VisitStatus.SUBTREE_VISIT_INTERRUPTED) {
			if (result == VisitStatus.VISIT_NOT_INTERRUPTED) {
				result = VisitStatus.SUBTREE_VISIT_INTERRUPTED;
			}
		}
		return result;
	}

}
