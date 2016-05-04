package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.qa.Explanation;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class ExplanationNavigation extends UndoableUIEvent<Explanation>{

	public ExplanationNavigation(Explanation e, String ui, boolean userInitiated) { super(e, ui, userInitiated); }

	public ExplanationNavigation(Trace trace, String[] args) {
		super(trace, args);
	}

	public void select(WhylineUI whylineUI) {
		
		whylineUI.selectExplanation(entity, false, UI.BACK_UI);
		
	}

	protected String getParsableStringArguments() { return Integer.toString(entity.getEventID()); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.EXPLANATION_NAVIGATION; }

	protected Explanation parseEntity(String[] args) { return null; }
	
	public Line getCorrespondingLine(Trace trace) {

		return trace.getInstruction(entity.getEventID()).getLine();
		
	}

}
