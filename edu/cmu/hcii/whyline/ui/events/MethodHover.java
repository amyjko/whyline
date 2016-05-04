package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */
public class MethodHover extends AbstractUIEvent<MethodInfo> {

	public MethodHover(MethodInfo method, String ui, boolean userInitiated) {
		
		super(method, ui, userInitiated);
		
	}

	public MethodHover(Trace trace, String[] args) {
		super(trace, args);
	}

	protected String getParsableStringArguments() { return Serializer.methodToString(entity); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.METHOD_HOVER; }

	protected MethodInfo parseEntity(String[] args) {
		
		return Serializer.stringToMethod(trace, args[FIRST_ARGUMENT_INDEX], args[FIRST_ARGUMENT_INDEX + 1]);
		
	}
	
}
