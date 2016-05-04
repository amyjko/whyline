package edu.cmu.hcii.whyline.ui.components;

import java.awt.BorderLayout;

import javax.swing.*;

/**
 * @author Andrew J. Ko
 *
 */
public class MultipleSplitPane extends WhylinePanel {

	private final int orientation;
	private JComponent first;
	private JComponent[] rest;
	private WhylineSplitPane[] splits;
	private JComponent componentToGiveResizeWeight = null;
	
	public MultipleSplitPane(int orientation, JComponent first, JComponent ... rest) {

		super(new BorderLayout());
		
		this.orientation = orientation;
		
		reset(first, rest);
				
	}
	
	public void resetWith(JComponent first, JComponent ... rest) { reset(first, rest); }
	
	private void reset(JComponent first, JComponent[] rest) {
		
		removeAll();
		
		this.first = first;
		this.rest = rest;

		splits = new WhylineSplitPane[rest.length];

		// If there's only one component, just add it.
		if(rest.length == 0)
			add(first, BorderLayout.CENTER);
		// If there's more than one, create a split with the first, and the rest.
		else {
			splits[0] = make(first, make(0));
			add(splits[0], BorderLayout.CENTER);
		}
		
		revalidate();
		
	}
	
	public void giveResizeWeightTo(JComponent c) {
		
		componentToGiveResizeWeight = c;
		updateResizeWeights();
		
	}
	
	private void updateResizeWeights() {
		
		for(WhylineSplitPane split : splits) {
			if(componentToGiveResizeWeight == null)
				split.setResizeWeight(.5);
			else {
				if(split.getLeftComponent() == componentToGiveResizeWeight)
					split.setResizeWeight(1.0);
				else
					split.setResizeWeight(0.0);
			}
		}

	}
	
	private JComponent make(int index) {

		if(index == rest.length - 1) return rest[index];
		else {
			splits[index + 1] = make(rest[index], index  + 1 == rest.length - 1 ? rest[index + 1] : make(index + 1));
			return splits[index + 1];
		}
		
	}
	
	private WhylineSplitPane make(JComponent one, JComponent two) {
		
		WhylineSplitPane split = new WhylineSplitPane(orientation, one, two);
		split.setResizeWeight(.5);
		split.setDividerLocation(.5);
		return split;
		
	}
	
	public void setDividers() {
		
		for(WhylineSplitPane split : splits)
			split.setDividerLocation(.5);
		revalidate();
		
	}
	
}