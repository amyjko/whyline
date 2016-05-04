package edu.cmu.hcii.whyline.ui.qa;

import java.awt.Color;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.views.View;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class ArgumentEventView extends EventView {

	public ArgumentEventView(Visualization visualization, Explanation explanation) {
		
		super(visualization, explanation);

		setLocalHeight(heightOfLabel, false);

	}

	public boolean isLastVisibleArgument() {
		
		View childAfter = getChildAfter();
		if(isHidden()) return false;
		if(!(childAfter instanceof ArgumentEventView)) return true;
		return !((ArgumentEventView)childAfter).isLastVisibleArgument();
		
	}
	
	public String determineFirstLine() { 

		String name = visualization.getTrace().getArgumentNameSet(getEventID());
		if(name == null) return "=";
		else return Util.elide(name, UI.MAX_EVENT_LENGTH) + " ="; 
		
	}
	
	public String determineSecondLine() { return Util.elide(visualization.getTrace().getArgumentValueDescription(getEventID()), UI.MAX_EVENT_LENGTH); }
	
	protected Color determineBorderColor() { return UI.DATA_COLOR; }
	
}
