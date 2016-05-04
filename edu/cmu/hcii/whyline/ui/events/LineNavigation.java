package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class LineNavigation extends UndoableUIEvent<Line>{

	public LineNavigation(Line e, String ui, boolean userInitiated) { super(e,ui, userInitiated); }

	public LineNavigation(Trace trace, String[] args) {
		super(trace, args);
	}

	public void select(WhylineUI whylineUI) {
		
		whylineUI.selectLine(entity, false, ui);
		
	}

	protected String getParsableStringArguments() { return Serializer.lineToString(entity); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.LINE_NAVIGATION; }

	protected Line parseEntity(String[] args) {

		return Serializer.stringToLine(trace, args[FIRST_ARGUMENT_INDEX], args[FIRST_ARGUMENT_INDEX + 1]);
	
	}

	public Line getCorrespondingLine(Trace trace) {

		return entity;
		
	}

}
