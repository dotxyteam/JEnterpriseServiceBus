package com.otk.jesb.util;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class TransparentPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private float opacity = 0.5f;

	public TransparentPanel() {
		setOpaque(false);
	}

	@Override
	public void paint(Graphics g) {
		if (opacity == 1.0) {
			super.paint(g);
			return;
		}
		BufferedImage image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
		super.paint(image.getGraphics());
		Graphics2D g2d = (Graphics2D) g;
		AlphaComposite newComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
		Composite compositeTorestore = g2d.getComposite();
		g2d.setComposite(newComposite);
		g2d.drawImage(image, 0, 0, null);
		g2d.setComposite(compositeTorestore);
	}

	public void setOpacity(float f) {
		this.opacity = f;
		repaint();
	}

	public float getOpacity() {
		return opacity;
	}

	@Override
	protected boolean isPaintingOrigin() {
		return true;
	}

}
