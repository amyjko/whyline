package edu.cmu.hcii.whyline.ui.components;

import javax.swing.Action;
import javax.swing.JToggleButton;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineToggleButton extends JToggleButton {

	public WhylineToggleButton(String text, float fontsize, Action action) {
		
		super(action);
		
		setFont(UI.getMediumFont().deriveFont(fontsize));
		setText(text);

		setBackground(UI.getControlBackColor());
		setForeground(UI.getControlTextColor());
		setFocusable(false);
		setOpaque(false);
		setBorder(new WhylineControlBorder());

	}
	
	public WhylineToggleButton(Action abstractAction) {
		
		super(abstractAction);
		
		setFont(UI.getMediumFont());

		setBackground(UI.getControlBackColor());
		setForeground(UI.getControlTextColor());
		setFocusable(false);
		setOpaque(false);
		setBorder(new WhylineControlBorder());
		
	}
	

}