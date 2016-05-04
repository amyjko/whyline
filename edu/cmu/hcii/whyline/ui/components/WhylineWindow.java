package edu.cmu.hcii.whyline.ui.components;

import java.awt.Color;

import javax.swing.JFrame;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineWindow extends JFrame {

	public WhylineWindow() {
		
		super();
		
		getContentPane().setBackground(null);
		
	}
	
	public Color getBackground() { return UI.getPanelLightColor(); }

}