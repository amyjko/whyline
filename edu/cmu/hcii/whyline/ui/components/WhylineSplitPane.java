package edu.cmu.hcii.whyline.ui.components;

import java.awt.Color;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineSplitPane extends JSplitPane {
	
	public WhylineSplitPane(int orientation, JComponent one, JComponent two) {

		super(orientation, true, one, two);
		
		setBorder(new EmptyBorder(0,0,0,0));
	
	}
	
	public Color getBackground() { return UI.getPanelLightColor(); }

}