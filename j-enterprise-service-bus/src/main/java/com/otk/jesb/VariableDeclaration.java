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

	public static class BasicVariableDeclaration implements VariableDeclaration {

		private String variableName;
		private Class<?> variableType;

		public BasicVariableDeclaration(String variableName, Class<?> variableType) {
			this.variableName = variableName;
			this.variableType = variableType;
		}

		@Override
		public String getVariableName() {
			return variableName;
		}

		@Override
		public Class<?> getVariableType() {
			return variableType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((variableName == null) ? 0 : variableName.hashCode());
			result = prime * result + ((variableType == null) ? 0 : variableType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BasicVariableDeclaration other = (BasicVariableDeclaration) obj;
			if (variableName == null) {
				if (other.variableName != null)
					return false;
			} else if (!variableName.equals(other.variableName))
				return false;
			if (variableType == null) {
				if (other.variableType != null)
					return false;
			} else if (!variableType.equals(other.variableType))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "BasicVariableDeclaration [variableName=" + variableName + ", variableType=" + variableType + "]";
		}

	}

}