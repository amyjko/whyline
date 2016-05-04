package edu.cmu.hcii.whyline.ui.components;

import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.border.EmptyBorder;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineMenuItem extends JMenuItem {

	public WhylineMenuItem(String name) {
		
		this(name, null);
		
	}
	
	public WhylineMenuItem(String name, ActionListener listener) {
		
		super(name);
		
		setBackground(UI.getPanelLightColor());
		setForeground(UI.getControlTextColor());

		setOpaque(false);

		setFont(UI.getMediumFont());
		
		setBorder(new EmptyBorder(0, 0, 0, 0));

		if(listener != null) addActionListener(listener);
		
	}
	
}
