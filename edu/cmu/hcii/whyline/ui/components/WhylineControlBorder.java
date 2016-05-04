/**
 * 
 */
package edu.cmu.hcii.whyline.ui.components;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.border.Border;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineControlBorder implements Border {

	private int padding = UI.getBorderPadding();
	
	public WhylineControlBorder() {}

	private Insets standardInsets = new Insets(padding, padding, padding, padding);
	
	public Insets getBorderInsets(Component c) {
		return standardInsets;
	}

	public boolean isBorderOpaque() {
		return true;
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {

		if(!c.isVisible()) return;
		
		Graphics2D g2 = (Graphics2D)g.create();

		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		Stroke old = g2.getStroke();

		g2.setStroke(new BasicStroke(UI.getBorderPadding()));
		g2.setColor(UI.getPanelLightColor());
		g2.drawRect(0, 0, width, height);

		int corner = UI.getRoundedness() - 1;
		
		int widthOfControlBackgroundStroke = UI.getBorderPadding() + 1;
		g2.setStroke(new BasicStroke(widthOfControlBackgroundStroke));
		g2.setColor(c.getBackground());
		g2.drawRoundRect(
			widthOfControlBackgroundStroke / 2 - 1, 
			widthOfControlBackgroundStroke / 2 - 1, 
			width - widthOfControlBackgroundStroke + 1, 
			height - widthOfControlBackgroundStroke + 1, 
			corner, corner);

		g2.setStroke(old);

		if(c.hasFocus()) {
			g2.setColor(UI.getFocusColor());
			float strokeWidth = 2.0f;
			g2.setStroke(new BasicStroke(strokeWidth));
			g2.drawRect((int)(strokeWidth / 2), (int)(strokeWidth / 2), (int)(width - strokeWidth - 1), (int)(height - strokeWidth - 1));
		}
		else {
			g2.setColor(c.isEnabled() ? UI.getControlBorderColor() : UI.getControlBorderColor().darker());
			g2.drawRoundRect(0, 0, width - 1, height - 1, corner, corner);
		}
		
	}
	
}