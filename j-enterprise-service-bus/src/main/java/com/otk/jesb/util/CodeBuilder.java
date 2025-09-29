package com.otk.jesb.util;

import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.List;

public class CodeBuilder {

	private static final char LINE_SEPARATOR = '\n';

	private final StringBuilder stringBuilder = new StringBuilder();
	private String indentationString = "\t";
	private int currentIndentation = 0;
	private List<PlaceHolder> placeholders = new ArrayList<CodeBuilder.PlaceHolder>();

	public CodeBuilder() {
		this("");
	}

	public CodeBuilder(String s) {
		append(s);
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

	private CodeBuilder appendIndentation() {
		for (int i = 0; i < currentIndentation; i++) {
			stringBuilder.append(indentationString);
		}
		return this;
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

	public CodeBuilder indenting(Runnable runnable) {
		int initialIndentation = currentIndentation;
		incrementIndentation();
		try {
			runnable.run();
		} finally {
			currentIndentation = initialIndentation;
		}
		return this;
	}

	public CodeBuilder appendIndented(String string) {
		indenting(() -> {
			append(string);
		});
		return this;
	}

	public CodeBuilder appendPlaceHolder(PlaceHolder placeholder) {
		registerPlaceHolder(placeholder);
		append(placeholder.getReferenceString());
		return this;
	}

	public CodeBuilder registerPlaceHolder(PlaceHolder placeholder) {
		placeholders.add(placeholder);
		return this;
	}

	public String toString() {
		String result = stringBuilder.toString();
		for (PlaceHolder placeHolder : placeholders) {
			result = placeHolder.replace(result);
		}
		return result;
	}

	public static abstract class PlaceHolder {

		public static final PlaceHolder NULL_PLACEHOLDER = new PlaceHolder() {

			@Override
			public String getReferenceString() {
				return "";
			}

			@Override
			protected String computeReplacement(String finalCode) {
				return finalCode;
			}
		};

		private final String referenceString = "$" + new UID().toString();

		protected abstract String computeReplacement(String finalCode);

		public String getReferenceString() {
			return referenceString;
		}

		protected String replace(String finalCode) {
			return finalCode.replace(referenceString, computeReplacement(finalCode));
		}

	}

}
