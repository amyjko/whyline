package edu.cmu.hcii.whyline.ui.components;

import javax.swing.Action;
import javax.swing.JRadioButton;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineRadioButton extends JRadioButton {

	public WhylineRadioButton(Action action) {
		
		super(action);
		
		setFont(UI.getMediumFont());
		setBackground(UI.getControlBackColor());
//		setForeground(UIConstants.CONTROL_TEXT_COLOR);
		setOpaque(false);
		setFocusable(false);
		
	}
	
}