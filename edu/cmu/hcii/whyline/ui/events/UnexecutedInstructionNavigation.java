package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.qa.UnexecutedInstruction;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class UnexecutedInstructionNavigation extends UndoableUIEvent<UnexecutedInstruction>{

	public UnexecutedInstructionNavigation(UnexecutedInstruction e, String ui, boolean userInitiated) { super(e, ui, userInitiated); }

	public UnexecutedInstructionNavigation(Trace trace, String[] args) {
		super(trace, args);
	}

	public void select(WhylineUI whylineUI) {
		
		whylineUI.selectUnexecutedInstruction(entity, false, UI.BACK_UI);
		
	}

	protected String getParsableStringArguments() { return Serializer.instructionToString(entity.getInstruction()); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.UNEXECUTED_NAVIGATION; }

	protected UnexecutedInstruction parseEntity(String[] args) {

		return null;
		
	}

	public Line getCorrespondingLine(Trace trace) {

		return entity.getInstruction().getLine();
		
	}

}
