package edu.cmu.hcii.whyline.ui.components;

import java.awt.Font;

import javax.swing.JLabel;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineLabel extends JLabel {

	public WhylineLabel(String text) {
		
		super(text);

		setForeground(UI.getPanelTextColor());
		setBackground(null);
		setFont(UI.getMediumFont());
		setOpaque(false);
		
	}

	public WhylineLabel(String label, float fontSize) {
		
		this(label);

		setFont(UI.getMediumFont().deriveFont(fontSize));
		
	}

	public WhylineLabel(String label, Font font) {
		
		this(label);

		setFont(font);
		
	}

}
