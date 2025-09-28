package com.otk.jesb.util;

public class CodeBuilder {

	private static final char LINE_SEPARATOR = '\n';

	private final StringBuilder stringBuilder = new StringBuilder();
	private final String indentationString;
	private int currentIndentation = 0;

	public CodeBuilder() {
		this("\t");
	}

	public CodeBuilder(String indentationString) {
		this.indentationString = indentationString;
	}

	public int getCurrentIndentation() {
		return currentIndentation;
	}

	public void setCurrentIndentation(int currentIndentation) {
		this.currentIndentation = currentIndentation;
	}

	public String getIndentationString() {
		return indentationString;
	}

	public void incrementIndentation() {
		currentIndentation++;
	}

	private void appendIndentation() {
		for (int i = 0; i < currentIndentation; i++) {
			stringBuilder.append(indentationString);
		}
	}

	public CodeBuilder append(String string) {
		if (string.isEmpty()) {
			return this;
		}
		int length = stringBuilder.length();
		if (length == 0) {
			appendIndentation();
		} else {
			char lastCharacter = stringBuilder.charAt(length - 1);
			if (lastCharacter == LINE_SEPARATOR) {
				appendIndentation();
			}
		}
		char lastCharacter = 0;
		for (int i = 0; i < string.length(); i++) {
			char character = string.charAt(i);
			if (lastCharacter == LINE_SEPARATOR) {
				appendIndentation();
			}
			stringBuilder.append(character);
			lastCharacter = character;
		}
		return this;
	}

	public void indenting(Runnable runnable) {
		int initialIndentation = currentIndentation;
		incrementIndentation();
		try {
			runnable.run();
		} finally {
			currentIndentation = initialIndentation;
		}
	}

	public void appendIndented(String string) {
		indenting(() -> {
			append(string);
		});
	}

	public String toString() {
		return stringBuilder.toString();
	}

}
