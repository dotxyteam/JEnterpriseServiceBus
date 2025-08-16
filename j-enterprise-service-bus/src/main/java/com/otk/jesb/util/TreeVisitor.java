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
	 * @param <T>
	 * @param t                The base node of the visit.
	 * @param visitor          The visitor.
	 * @param childrenAccessor A function that retrieves the children of the given
	 *                         node.
	 * @return the final visit status. Note that if at least 1 descendant subtree
	 *         visit was interrupted, then the final status will necessarily be
	 *         {@link VisitStatus#SUBTREE_VISIT_INTERRUPTED} or
	 *         {@link VisitStatus#TREE_VISIT_INTERRUPTED}.
	 */
	public static <T> TreeVisitor.VisitStatus visitTreeFrom(T t, TreeVisitor<T> visitor,
			Function<T, Iterable<? extends T>> childrenAccessor) {
		TreeVisitor.VisitStatus finalItemVisitStatus = visitor.visitNode(t);
		if (finalItemVisitStatus == VisitStatus.TREE_VISIT_INTERRUPTED) {
			return VisitStatus.TREE_VISIT_INTERRUPTED;
		}
		if (finalItemVisitStatus != VisitStatus.SUBTREE_VISIT_INTERRUPTED) {
			for (T child : childrenAccessor.apply(t)) {
				VisitStatus childItemVisitStatus = visitTreeFrom(child, visitor, childrenAccessor);
				if (childItemVisitStatus == VisitStatus.TREE_VISIT_INTERRUPTED) {
					return VisitStatus.TREE_VISIT_INTERRUPTED;
				}
				if (childItemVisitStatus == VisitStatus.SUBTREE_VISIT_INTERRUPTED) {
					if (finalItemVisitStatus == VisitStatus.VISIT_NOT_INTERRUPTED) {
						finalItemVisitStatus = VisitStatus.SUBTREE_VISIT_INTERRUPTED;
					}
				}
			}
		}
		return finalItemVisitStatus;
	}

}
