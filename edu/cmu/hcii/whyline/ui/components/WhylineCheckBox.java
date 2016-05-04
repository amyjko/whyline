package edu.cmu.hcii.whyline.ui.components;

import javax.swing.Action;
import javax.swing.JCheckBox;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineCheckBox extends JCheckBox {

	public WhylineCheckBox(String name) {
		
		super(name);
		
		setFont(UI.getSmallFont());
		setFocusable(false);
		setOpaque(false);
		setBorder(null);
		
	}
	
	public WhylineCheckBox(Action abstractAction) {
		
		this("");

		setAction(abstractAction);
		
	}
	
}