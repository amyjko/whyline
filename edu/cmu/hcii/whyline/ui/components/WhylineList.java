package edu.cmu.hcii.whyline.ui.components;

import javax.swing.JList;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineList extends JList {

	public WhylineList() {
		
		setFont(UI.getMediumFont());
		setFocusable(false);
		setForeground(UI.getControlTextColor());
		setBackground(UI.getControlBackColor());
		
		setOpaque(true);
		
		setBorder(null);
		
	}
	
}
