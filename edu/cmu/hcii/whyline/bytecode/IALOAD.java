package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IALOAD extends GetArrayValue {

	public IALOAD(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 46; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.INTEGER_PRODUCED; }

}
