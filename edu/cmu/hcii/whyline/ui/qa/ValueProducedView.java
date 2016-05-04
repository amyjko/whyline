package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class ValueProducedView extends EventView {

	public ValueProducedView(Visualization visualization, Explanation explanation) {
		
		super(visualization, explanation);
		
	}

	protected Color determineBorderColor() { return UI.IO_COLOR; }

	public String determineFirstLine() {
		
		String label = visualization.getTrace().getDescription(getEventID()); 
		int index = Util.findStringSplitIndex(label);
		if(index >= 0)
			return label.substring(0, index);
		else
			return label;

	}

	public String determineSecondLine() {
		
		String label = visualization.getTrace().getDescription(getEventID()); 
		int index = Util.findStringSplitIndex(label);
		if(index >= 0)
			return label.substring(index);
		else
			return null;
		
	}
	
}