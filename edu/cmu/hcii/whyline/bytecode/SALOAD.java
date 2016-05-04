package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class SALOAD extends GetArrayValue {

	public SALOAD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 53; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.SHORT_PRODUCED; }

}
