package edu.cmu.hcii.whyline.ui.components;

import javax.swing.JTable;

import edu.cmu.hcii.whyline.ui.UI;

/**
 * @author Andrew J. Ko
 *
 */
public class WhylineTable extends JTable {

	public WhylineTable() {

		setForeground(UI.getControlTextColor());
		setBackground(UI.getControlBackColor());
		setFocusable(false);

	}
	
}
