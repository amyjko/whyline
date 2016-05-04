package edu.cmu.hcii.whyline.ui.components;

import javax.swing.JPopupMenu;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylinePopup extends JPopupMenu {

	public WhylinePopup(String name) {
		
		super(name);
		
		setBackground(UI.getPanelLightColor());
		setForeground(UI.getPanelTextColor());

		setBorder(new WhylineMenuBorder());

	}
	
}
