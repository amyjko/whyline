/**
 * 
 */
package edu.cmu.hcii.whyline.ui.components;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.Border;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineMenuBorder implements Border {
	
	public WhylineMenuBorder() {}

	private Insets standardInsets = new Insets(UI.getBorderPadding(), UI.getBorderPadding(), UI.getBorderPadding(), UI.getBorderPadding());
	
	public Insets getBorderInsets(Component c) {
		return standardInsets;
	}

	public boolean isBorderOpaque() {
		return true;
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

		Graphics2D g2 = (Graphics2D)g.create();

		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		
		g2.setColor(UI.getControlBorderColor());
		g2.drawRect(0, 0, width - 1, height - 1);
		
	}
	
}