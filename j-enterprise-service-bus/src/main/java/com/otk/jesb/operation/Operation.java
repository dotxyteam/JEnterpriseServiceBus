package com.otk.jesb.operation;

/**
 * This is the base of all atomic activity in a {@link Plan}.
 * 
 * @author olitank
 *
 */
public interface Operation {

	Object execute() throws Throwable;

}