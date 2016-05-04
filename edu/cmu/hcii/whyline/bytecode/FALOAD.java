package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class FALOAD extends GetArrayValue {

	public FALOAD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 48; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.FLOAT_PRODUCED; }

}
