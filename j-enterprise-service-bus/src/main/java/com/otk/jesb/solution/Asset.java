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

	/**
	 * @return The asset name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Updates the asset name.
	 * 
	 * @param name The new asset name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return The comment associated with the current asset (may be null).
	 */
	public String getNote() {
		return note;
	}

	/**
	 * Updates the comment associated with the current asset.
	 * 
	 * @param note The new note (may be null).
	 */
	public void setNote(String note) {
		this.note = note;
	}

	/**
	 * @return The name with (if applicable) the extension.
	 */
	public String getFullName() {
		if (this instanceof Folder) {
			return (name != null) ? name : "";
		} else {
			return ((name != null) ? name : "") + "." + getClass().getSimpleName().toLowerCase();
		}
	}

	/**
	 * @return The name of the associated resource on the file system.
	 */
	public String getFileSystemResourceName() {
		if (this instanceof Folder) {
			return getFullName();
		} else {
			return getFullName() + MiscUtils.SERIALIZED_FILE_NAME_SUFFIX;
		}
	}

	/**
	 * Checks the consistency of the current asset data.
	 * 
	 * @param recursively      Whether checks should also be performed on any
	 *                         sub-elements.
	 * @param solutionInstance The current solution.
	 * @throws ValidationError If an inconsistency is detected.
	 */
	public void validate(boolean recursively, Solution solutionInstance) throws ValidationError {
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
