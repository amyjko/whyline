package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public class KeyInputParser extends ExecutionEventParser {
	
	public KeyInputParser(Trace trace) {
		super(trace);
	}

	public static boolean handles(Instruction inst) {

		return false;

	}
	
	public boolean handle(int keyID) {

		if(trace.getKind(keyID) == EventKind.KEY_EVENT) {
		
			trace.getKeyHistory().add(new KeyStateInputEvent(trace, keyID));
			return true;

		}
		else return false;
		
	}

}
