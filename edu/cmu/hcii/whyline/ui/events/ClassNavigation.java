package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.bytecode.Classfile;
import edu.cmu.hcii.whyline.source.FileInterface;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.ParseException;
import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class ClassNavigation extends UndoableUIEvent<Classfile>{

	public ClassNavigation(Classfile e, String ui, boolean userInitiated) { super(e, ui, userInitiated); }

	public ClassNavigation(Trace trace, String[] args) {
		super(trace, args);
	}

	public void select(WhylineUI whylineUI) {
		
		whylineUI.selectClass(entity, false, UI.BACK_UI);
		
	}

	protected String getParsableStringArguments() { return Serializer.classfileToString(entity); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.CLASS_NAVIGATION; }

	protected Classfile parseEntity(String[] args) {
		
		return Serializer.stringToClassfile(trace, args[FIRST_ARGUMENT_INDEX]);
		
	}

	public Line getCorrespondingLine(Trace trace) throws ParseException {
		
		FileInterface file = entity.getSourceFile();
		if(file == null) file = entity;
		return file.getLine(1);
		
	}
	
}
