package edu.cmu.hcii.whyline.ui.components;

import java.awt.Color;

import javax.swing.JToolBar;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineToolbar extends JToolBar {

	public WhylineToolbar(int orientation) {
		
		super(orientation);
		
		setFloatable(false);
		setOpaque(false);
		setBorder(null);
		setRollover(true);
		
	}
	
	public Color getBackground() { return UI.getPanelDarkColor(); }
	
}