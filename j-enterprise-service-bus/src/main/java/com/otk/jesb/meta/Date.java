package com.otk.jesb.meta;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.MiscUtils;

public class Date {

	private static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	public static final Date toDay() {
		return fromJavaUtilDate(MiscUtils.now());
	}

	private final String stringRepresentation;

	public Date() {
		this(toDay().getStringRepresentation());
	}

	public Date(String stringRepresentation) {
		this.stringRepresentation = stringRepresentation;
	}

	public String getStringRepresentation() {
		return stringRepresentation;
	}

	public java.util.Date toJavaUtilDate() {
		try {
			return SIMPLE_DATE_FORMAT.parse(stringRepresentation);
		} catch (ParseException e) {
			throw new UnexpectedError(e);
		}
	}

	public static Date fromJavaUtilDate(java.util.Date date) {
		return new Date(SIMPLE_DATE_FORMAT.format(date));
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

}
