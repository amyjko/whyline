package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class NoLineHover extends AbstractUIEvent<Object> {

	public NoLineHover(String ui) {

		super(null, ui, false);
		
	}

	public NoLineHover(Trace trace, String[] args) {
		super(trace, args);
	}

	protected String getParsableStringArguments() { return ""; }
	protected UIEventKind getParsableStringKind() { return UIEventKind.NO_LINE_HOVER; }
	public void select(WhylineUI whylineUI) {}

	protected Object parseEntity(String[] args) {

		return null;
	
	}
	
}
