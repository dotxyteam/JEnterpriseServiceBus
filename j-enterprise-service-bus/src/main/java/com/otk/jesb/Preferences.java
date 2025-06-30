package com.otk.jesb;

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
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme;
import com.otk.jesb.ui.GUI.BetterFlatTableHeaderUI;
import com.otk.jesb.util.MiscUtils;

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

	public enum Theme {
		SYSTEM, CROSS_PLATFORM, FLAT;

		public void activate() {
			try {
				if (this == Theme.SYSTEM) {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} else if (this == Theme.CROSS_PLATFORM) {
					UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				} else if (this == Theme.FLAT) {
					UIManager.setLookAndFeel(new FlatLightFlatIJTheme() {

						private static final long serialVersionUID = 1L;

						{
							try (InputStream is = Preferences.class.getResourceAsStream("ui/FlatLightFlatIJTheme.ttf")) {
								Font font = Font.createFont(Font.TRUETYPE_FONT, is);
								GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
							} catch (IOException | FontFormatException e) {
								throw new UnexpectedError(e);
							}
							FlatLightFlatIJTheme
									.registerCustomDefaultsSource(Preferences.class.getPackage().getName() + ".ui");
						}

						@Override
						public UIDefaults getDefaults() {
							UIDefaults result = super.getDefaults();
							result.put("TableHeaderUI", BetterFlatTableHeaderUI.class.getName());
							return result;
						}
					});
				} else {
					throw new UnexpectedError();
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e) {
				throw new UnexpectedError(e);
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (Window window : Window.getWindows()) {
						SwingUtilities.updateComponentTreeUI(window);
					}
				}
			});
		}
	}

}
