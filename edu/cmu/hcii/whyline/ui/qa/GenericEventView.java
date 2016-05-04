package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class GenericEventView extends EventView {

	private final Color color;

	public GenericEventView(Visualization visualization, Explanation explanation) {
		
		super(visualization, explanation);

		EventKind kind = visualization.getTrace().getKind(explanation.getEventID());
		
		if(kind.isInvocation) color = UI.CONTROL_COLOR;
		else if(kind.isBranch) color = UI.CONTROL_COLOR;
		else if(kind == EventKind.EXCEPTION_CAUGHT) color = UI.CONTROL_COLOR;
		else if(kind == EventKind.EXCEPTION_THROWN) color = UI.CONTROL_COLOR;
		else if(kind == EventKind.RETURN) color = UI.DATA_COLOR;
		else if(kind.isDefinition) color = UI.DATA_COLOR;
		else if(kind == EventKind.NEW_OBJECT) color = UI.DATA_COLOR;
		else color = UI.DATA_COLOR;

	}

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
	
	protected Color determineBorderColor() { return color; }
	
}
