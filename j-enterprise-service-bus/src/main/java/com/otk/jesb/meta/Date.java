package com.otk.jesb.meta;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.otk.jesb.PotentialError;
import com.otk.jesb.util.MiscUtils;

public class Date {

	private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	public static final Date toDay() {
		return fromJavaUtilDate(MiscUtils.now());
	}

	private final String stringRepresentation;

	public Date() {
		this.stringRepresentation = toDay().getStringRepresentation();
	}

	public Date(String stringRepresentation) {
		this.stringRepresentation = stringRepresentation;
		// validate format
		toJavaUtilDate();
	}

	public String getStringRepresentation() {
		return stringRepresentation;
	}

	public java.util.Date toJavaUtilDate() {
		try {
			return DATE_FORMAT.parse(stringRepresentation);
		} catch (ParseException e) {
			throw new PotentialError("'" + stringRepresentation + "' format is invalid: Expected date format: "
					+ DATE_FORMAT.toPattern() + " (example: " + toDay().getStringRepresentation() + ")", e);
		}
	}

	public static Date fromJavaUtilDate(java.util.Date date) {
		return new Date(DATE_FORMAT.format(date));
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

}
