package com.otk.jesb;

/**
 * Allows to associate an identifier (name) with a value.
 * 
 * @author olitank
 *
 */
public interface Variable {

	public static final Object UNDEFINED_VALUE = new Object() {

		@Override
		public String toString() {
			return Variable.class.getName() + ".UNDEFINED_VALUE";
		}
	};

	Object getValue();

	String getName();

	public static class BasicVariable implements Variable {

		private String name;
		private Object value;

		public BasicVariable(String name, Object value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			BasicVariable other = (BasicVariable) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "BasicVariable [name=" + name + ", value=" + value + "]";
		}

	}

	public static class Proxy implements Variable {

		private Variable base;

		public Proxy(Variable base) {
			this.base = base;
		}

		public Object getValue() {
			return base.getValue();
		}

		public String getName() {
			return base.getName();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((base == null) ? 0 : base.hashCode());
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
			Proxy other = (Proxy) obj;
			if (base == null) {
				if (other.base != null)
					return false;
			} else if (!base.equals(other.base))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "VariableProxy [base=" + base + "]";
		}

	}

}