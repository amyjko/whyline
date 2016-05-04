package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class LineHover extends AbstractUIEvent<Line> {

	public LineHover(Line line, String ui) {

		super(line, ui, false);
		
	}

	public LineHover(Trace trace, String[] args) {
		super(trace, args);
	}

	protected String getParsableStringArguments() { return Serializer.lineToString(entity); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.LINE_HOVER; }
	public void select(WhylineUI whylineUI) {}

	protected Line parseEntity(String[] args) {

		return Serializer.stringToLine(trace, args[FIRST_ARGUMENT_INDEX], args[FIRST_ARGUMENT_INDEX + 1]);
	
	}
	
}
