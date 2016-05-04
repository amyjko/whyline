package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class DefinitionEventView extends EventView {

	public DefinitionEventView(Visualization visualization, Explanation explanation) {
		
		super(visualization, explanation);
		
	}

	public String determineFirstLine() { 
		
		String desc = visualization.getTrace().getDescription(getEventID());
		return Util.elide(desc.substring(0, desc.indexOf('=')), UI.MAX_EVENT_LENGTH) + "=";
		
	}
	public String determineSecondLine() { 

		String desc = visualization.getTrace().getDescription(getEventID());
		return Util.elide(desc.substring(desc.indexOf('=') + 1), UI.MAX_EVENT_LENGTH);

	}
	
	protected Color determineBorderColor() { return UI.DATA_COLOR; }
	
}
