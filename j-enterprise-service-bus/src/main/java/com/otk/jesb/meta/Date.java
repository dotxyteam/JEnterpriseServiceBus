package com.otk.jesb.meta;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.MiscUtils;

public class Date {

	public static final Date TODAY;
	static {
		try {
			TODAY = fromJavaUtilDate(MiscUtils.now());
		} catch (ParseException e) {
			throw new UnexpectedError(e);
		}
	};

	private static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	private final String stringRepresentation;

	public Date(String stringRepresentation) {
		this.stringRepresentation = stringRepresentation;
	}

	public String getStringRepresentation() {
		return stringRepresentation;
	}

	public java.util.Date toJavaUtilDate() throws ParseException {
		return SIMPLE_DATE_FORMAT.parse(stringRepresentation);
	}

	public static Date fromJavaUtilDate(java.util.Date date) throws ParseException {
		return new Date(SIMPLE_DATE_FORMAT.format(date));
	}

	@Override
	public String toString() {
		return stringRepresentation;
	}

}
