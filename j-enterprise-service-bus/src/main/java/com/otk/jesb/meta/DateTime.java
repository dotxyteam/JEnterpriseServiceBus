package com.otk.jesb.meta;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.MiscUtils;

public class DateTime {

	private static SimpleDateFormat JAVA_UTIL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public static final DateTime NOW;
	static {
		try {
			NOW = fromJavaUtilDate(MiscUtils.now());
		} catch (ParseException e) {
			throw new UnexpectedError(e);
		}
	};

	private final String stringRepresentation;

	public DateTime(String stringRepresentation) {
		this.stringRepresentation = stringRepresentation;
	}

	public String getStringRepresentation() {
		return stringRepresentation;
	}

	public java.util.Date toJavaUtilDate() throws ParseException {
		return JAVA_UTIL_DATE_FORMAT.parse(stringRepresentation);
	}

	public static DateTime fromJavaUtilDate(java.util.Date date) throws ParseException {
		return new DateTime(JAVA_UTIL_DATE_FORMAT.format(date));
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

}
