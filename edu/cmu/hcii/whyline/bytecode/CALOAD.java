package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class CALOAD extends GetArrayValue {

	public CALOAD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 52; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.CHARACTER_PRODUCED; }

}
