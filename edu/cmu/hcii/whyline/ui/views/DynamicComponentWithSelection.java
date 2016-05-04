package edu.cmu.hcii.whyline.ui.views;

import java.awt.Frame;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class DynamicComponentWithSelection<SelectionType> extends DynamicComponent {

	private SelectionType selection;
	
	public DynamicComponentWithSelection(Frame frame, Sizing width, Sizing height) {

		super(frame, width, height);

	}

	public void setSelection(SelectionType newSelection, boolean scroll) {
		
		setSelection(newSelection, scroll, null);
		
	}
	
	public void setSelection(SelectionType newSelection, boolean scroll, String ui) {

		// Reject views that aren't visible.
		if(newSelection instanceof View && ((View)newSelection).getContainer() == null) return;
		
		this.selection = newSelection;
		
		handleNewSelection(selection, scroll, ui);
		
		repaint();
		
	}

	public SelectionType getSelection() { return selection; }

	public abstract void handleNewSelection(SelectionType selection, boolean scroll, String ui);
	
}
