package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.source.FileInterface;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.ParseException;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class MethodNavigation extends UndoableUIEvent<MethodInfo>{

	public MethodNavigation(MethodInfo e, String ui, boolean userInitiated) { super(e, ui, userInitiated); }

	public MethodNavigation(Trace trace, String[] args) {
		super(trace, args);
	}

	public void select(WhylineUI whylineUI) {
		
		whylineUI.selectMethod(entity, false, UI.BACK_UI);
		
	}

	protected String getParsableStringArguments() { return Serializer.methodToString(entity); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.METHOD_NAVIGATION; }
	
	protected MethodInfo parseEntity(String[] args) {
		
		return Serializer.stringToMethod(trace, args[FIRST_ARGUMENT_INDEX], args[FIRST_ARGUMENT_INDEX + 1]);
		
	}

	public Line getCorrespondingLine(Trace trace) throws ParseException {

		FileInterface file = entity.getClassfile().getSourceFile();
		if(file == null) file = entity.getClassfile();
		Token t = file.getTokenForMethodName(entity);
		return t == null ? file.getLine(0) : t.getLine();
		
	}

}
