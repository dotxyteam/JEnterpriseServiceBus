package com.otk.jesb.meta;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.otk.jesb.PotentialError;
import com.otk.jesb.util.MiscUtils;

public class DateTime {

	private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static final DateTime now() {
		return fromJavaUtilDate(MiscUtils.now());
	}

	private final String stringRepresentation;

	public DateTime() {
		this.stringRepresentation = now().getStringRepresentation();
	}

	public DateTime(String stringRepresentation) {
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
			throw new PotentialError("'" + stringRepresentation + "' format is invalid: Expected date/time format: "
					+ DATE_FORMAT.toPattern() + " (example: " + now().getStringRepresentation() + ")", e);
		}
	}

	public static DateTime fromJavaUtilDate(java.util.Date date) {
		return new DateTime(DATE_FORMAT.format(date));
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

}
