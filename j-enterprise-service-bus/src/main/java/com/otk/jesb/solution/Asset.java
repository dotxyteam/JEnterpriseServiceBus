package com.otk.jesb.solution;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

import com.otk.jesb.ValidationError;
import com.otk.jesb.util.MiscUtils;

/**
 * This class is the base of any element of a {@link Solution}, persisted in a
 * separate resource of the file system (file or folder).
 * 
 * @author olitank
 *
 */
public abstract class Asset {

	private static final char[] ILLEGAL_NAME_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<',
			'>', '|', '\"', ':' };
	private String name;
	private String note;

	public Asset() {
		this.name = getClass().getSimpleName() + MiscUtils.getDigitalUniqueIdentifier();
	}

	public Asset(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getFullName() {
		if (this instanceof Folder) {
			return (name != null) ? name : "";
		} else {
			return ((name != null) ? name : "") + "." + getClass().getSimpleName().toLowerCase();
		}
	}

	public String getFileSystemResourceName() {
		if (this instanceof Folder) {
			return getFullName();
		} else {
			return getFullName() + MiscUtils.SERIALIZED_FILE_NAME_SUFFIX;
		}
	}

	public void validate(boolean recursively) throws ValidationError {
		if ((name == null) || (name.length() == 0)) {
			throw new ValidationError("Name not specified");
		}
		if ((name.matches("^\\s.*")) || (name.matches(".*\\s^"))) {
			throw new ValidationError("The name cannot start or end with a whitespace character");
		}
		for (int i = 0; i < ILLEGAL_NAME_CHARACTERS.length; i++) {
			if (name.contains(Character.toString(ILLEGAL_NAME_CHARACTERS[i]))) {
				throw new ValidationError("Invalid name detected: Illegal charcater found at position " + i + ": '"
						+ MiscUtils.escapeRegex(Character.toString(ILLEGAL_NAME_CHARACTERS[i])) + "'");
			}
		}
		try {
			Paths.get(name);
		} catch (InvalidPathException e) {
			throw new ValidationError("Invalid name detected: " + e.toString(), e);
		}
	}

	@Override
	public String toString() {
		return name;
	}

}
