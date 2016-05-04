package edu.cmu.hcii.whyline.ui.components;

import javax.swing.JLabel;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineTitleLabel extends JLabel {

	public WhylineTitleLabel(String text) {
		
		super(text);

		setForeground(UI.getPanelTextColor());
		setFont(UI.getLargeFont());
		
	}
	
}
