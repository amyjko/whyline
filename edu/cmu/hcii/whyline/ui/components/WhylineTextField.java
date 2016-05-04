package edu.cmu.hcii.whyline.ui.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.font.GlyphVector;

import javax.swing.JTextField;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineTextField extends JTextField {

	private GlyphVector label;
	private String labelText;

	public WhylineTextField(String defaultText, int numberOfColumns, String label) {

		super(defaultText, numberOfColumns);
		
		this.labelText = label;

		setBorder(new WhylineControlBorder());
		setColumns(20);
		setFont(UI.getMediumFont());
		setForeground(UI.getControlTextColor());
	
		setCaretColor(UI.getControlFrontColor());
		
		setOpaque(false);
		
		addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) { repaint(); }
			public void focusLost(FocusEvent e) { repaint(); }
		});

	}

	public void paintComponent(Graphics g) {

		if(!isVisible()) return;

		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(UI.getControlBackColor());
		g.fillRoundRect(getInsets().left / 2 + 1, getInsets().top / 2 + 1, getWidth() - getInsets().right - 1, getHeight() - getInsets().top - 1, UI.getRoundedness(), UI.getRoundedness());

		super.paintComponent(g);
				
		if(label == null)
			label = getFont().createGlyphVector(((Graphics2D)g).getFontRenderContext(), labelText);
		
		g = g.create();

		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		g.setColor(UI.getControlDisabledColor());
		if(getText().equals("")) {
		
			int heightWithoutInsets = getHeight() - getInsets().top;
			int labelHeight = (int) label.getLogicalBounds().getHeight();
			int offset = (heightWithoutInsets - labelHeight) / 2;
			int y = getHeight() - getInsets().top - offset + 1;
			
			((Graphics2D)g).drawGlyphVector(label, getInsets().left + 4, y); 
			
			
		}

	}

}
