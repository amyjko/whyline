package edu.cmu.hcii.whyline.ui.components;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class HeadlinedPanel extends WhylinePanel {

	protected final WhylineUI whylineUI;
	private final WhylineToolbar toolbar;
	protected final WhylinePanel content;
	
	public HeadlinedPanel(String header, WhylineUI whylineUI) {
		
		this.whylineUI = whylineUI;
		this.content = new WhylinePanel();

		toolbar = new WhylineToolbar(WhylineToolbar.HORIZONTAL);
		toolbar.add(new WhylineTitleLabel(header));

		setLayout(new BorderLayout(0, UI.getPanelPadding()));

		add(toolbar, BorderLayout.NORTH);
		
	}

	protected void addTool(JComponent action) {

		toolbar.addSeparator();
		toolbar.add(action);
		
	}
	
	protected void setContent(JComponent content) {
		
		add(content, BorderLayout.CENTER);
		revalidate();
		
	}
	
}
