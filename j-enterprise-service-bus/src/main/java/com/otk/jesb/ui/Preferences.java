package com.otk.jesb.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme;
import com.otk.jesb.Profile;
import com.otk.jesb.UnexpectedError;
import com.otk.jesb.util.MiscUtils;

import xy.reflect.ui.control.swing.customizer.SwingCustomizer;
import xy.reflect.ui.control.swing.util.SwingRendererUtils;

public class Preferences {

	private static final File FILE = new File(Profile.INSTANCE.getProfileDirectory(), "preferenes.xml");
	public static final Preferences INSTANCE;
	static {
		if (FILE.exists()) {
			try (FileInputStream fileInputStream = new FileInputStream(FILE)) {
				INSTANCE = (Preferences) MiscUtils.deserialize(fileInputStream);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
				throw new UnexpectedError();
			}
		} else {
			INSTANCE = new Preferences();
		}
	}

	private Preferences() {
	}

	private boolean fadingTransitioningEnabled = false;
	private boolean logVerbose = false;
	private Theme theme = Theme.FLAT;

	public void persist() {
		try (FileOutputStream fileOutputStream = new FileOutputStream(FILE)) {
			MiscUtils.serialize(this, fileOutputStream);
		} catch (IOException e) {
			throw new UnexpectedError(e);
		}
	}

	public boolean isFadingTransitioningEnabled() {
		return fadingTransitioningEnabled;
	}

	public void setFadingTransitioningEnabled(boolean fadingTransitioningEnabled) {
		this.fadingTransitioningEnabled = fadingTransitioningEnabled;
	}

	public Theme getTheme() {
		return theme;
	}

	public void setTheme(Theme theme) {
		theme.activate();
		this.theme = theme;
	}

	public boolean isLogVerbose() {
		return logVerbose;
	}

	public void setLogVerbose(boolean logVerbose) {
		this.logVerbose = logVerbose;
	}

	public enum Theme {
		SYSTEM, CROSS_PLATFORM, FLAT;

		public void activate() {
			try {
				if (this == Theme.SYSTEM) {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} else if (this == Theme.CROSS_PLATFORM) {
					UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				} else if (this == Theme.FLAT) {
					System.setProperty("awt.useSystemAAFontSettings", "on");
					System.setProperty("swing.aatext", "true");
					Font font;
					try (InputStream is = Preferences.class.getResourceAsStream("FlatLightFlatIJTheme.ttf")) {
						font = Font.createFont(Font.TRUETYPE_FONT, is);
						GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
						FlatLaf.setPreferredFontFamily(font.getFamily());
					} catch (IOException | FontFormatException e) {
						throw new UnexpectedError(e);
					}
					UIManager.put( "TableHeader.height", 0 );
					UIManager.setLookAndFeel(new FlatLightFlatIJTheme() );
				} else {
					throw new UnexpectedError();
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e) {
				throw new UnexpectedError(e);
			}
			if (Window.getWindows().length > 0) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						for (Window window : Window.getWindows()) {
							SwingUtilities.updateComponentTreeUI(window);
						}
						// force recreation of all controls to prevent some components painting issues
						for (SwingCustomizer customizer : GUI.INSTANCE.getSubCustomizerBySwitch().values()) {
							customizer.getCustomizationOptions()
									.setInEditMode(!customizer.getCustomizationOptions().isInEditMode());
							SwingRendererUtils.refreshAllDisplayedForms(customizer, true);
							customizer.getCustomizationOptions()
									.setInEditMode(!customizer.getCustomizationOptions().isInEditMode());
							SwingRendererUtils.refreshAllDisplayedForms(customizer, true);
						}
					}
				});
			}
		}

	}

}
