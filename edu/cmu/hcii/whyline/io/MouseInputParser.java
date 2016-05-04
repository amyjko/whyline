package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public class MouseInputParser extends ExecutionEventParser {
	
	public MouseInputParser(Trace trace) {
		super(trace);
	}

	public static boolean handles(Instruction inst) {

		return false;

	}
	
	public boolean handle(int mouseID) {

		if(trace.getKind(mouseID) == EventKind.MOUSE_EVENT) {
		
			trace.getMouseHistory().add(new MouseStateInputEvent(trace, mouseID));
			return true;

		}
		else return false;
		
	}

}
