package edu.cmu.hcii.whyline.ui.components;

import javax.swing.JTextArea;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineTextArea extends JTextArea {

	public WhylineTextArea(String defaultText, int rows, int columns) {

		super(defaultText, rows, columns);
		
		setBorder(new WhylineControlBorder());
		setFont(UI.getMediumFont());
		
		setOpaque(false);
		
	}

}
