package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class InstructionNavigation extends UndoableUIEvent<Instruction>{

	public InstructionNavigation(Instruction e, String ui, boolean userInitiated) { super(e, ui, userInitiated); }

	public InstructionNavigation(Trace trace, String[] args) {
		super(trace, args);
	}

	public void select(WhylineUI whylineUI) {
		
		whylineUI.selectInstruction(entity, false, UI.BACK_UI);
		
	}

	protected String getParsableStringArguments() { return Serializer.instructionToString(entity); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.INSTRUCTION_NAVIGATION; }

	protected Instruction parseEntity(String[] args) {
		
		return Serializer.stringtoInstruction(trace, args[FIRST_ARGUMENT_INDEX], args[FIRST_ARGUMENT_INDEX + 1], args[FIRST_ARGUMENT_INDEX + 2]);
	}

	public Line getCorrespondingLine(Trace trace) {

		return entity.getLine();
		
	}

}
