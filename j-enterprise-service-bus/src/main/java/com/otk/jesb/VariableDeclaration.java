package com.otk.jesb;

/**
 * Allows to anticipate the existence of a value of a certain type associated
 * with a specific identifier (name).
 * 
 * @author olitank
 *
 */
public interface VariableDeclaration {

	Class<?> getVariableType();

	String getVariableName();

}