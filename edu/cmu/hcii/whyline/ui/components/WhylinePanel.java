package edu.cmu.hcii.whyline.ui.components;

import java.awt.LayoutManager;

import javax.swing.JPanel;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylinePanel extends JPanel {

	public WhylinePanel(LayoutManager layout) {
		
		this();
		
		setLayout(layout);
		
	}
	
	public WhylinePanel() {

		super();
		
		setFocusable(false);
		setForeground(UI.getPanelTextColor());
		setBackground(null);
		setOpaque(false);
		setBorder(null);
		
	}
	
}