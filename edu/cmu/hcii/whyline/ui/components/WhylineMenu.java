package edu.cmu.hcii.whyline.ui.components;

import javax.swing.JMenu;
import javax.swing.border.EmptyBorder;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineMenu extends JMenu {

	public WhylineMenu(String name) {
		
		super(name);

		setFont(UI.getMediumFont());
		setBackground(UI.getPanelLightColor());
		setForeground(UI.getPanelTextColor());
		
		setBorder(new EmptyBorder(0, 0, 0, 0));

		getPopupMenu().setBackground(UI.getPanelLightColor());
		getPopupMenu().setBorder(new WhylineMenuBorder());
		
	}
	
}
