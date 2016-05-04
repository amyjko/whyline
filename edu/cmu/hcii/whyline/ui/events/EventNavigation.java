package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class EventNavigation extends UndoableUIEvent<Integer>{

	public EventNavigation(Integer e, String ui, boolean userInitiated) { super(e, ui, userInitiated); }

	public EventNavigation(Trace trace, String[] args) {
		super(trace, args);
	}

	public void select(WhylineUI whylineUI) {
		
		whylineUI.selectEvent(entity, false, UI.BACK_UI);
		
	}

	protected String getParsableStringArguments() { return entity.toString(); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.EVENT_NAVIGATION; }

	protected Integer parseEntity(String[] args) {
		
		return Integer.parseInt(args[FIRST_ARGUMENT_INDEX]);
		
	}

	public Line getCorrespondingLine(Trace trace) {

		return trace.getInstruction(entity).getLine();
		
	}

}
