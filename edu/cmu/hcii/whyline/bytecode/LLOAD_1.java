package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LLOAD_1 extends GetLocal {

	public LLOAD_1(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 1; }

	public final int getOpcode() { return 31; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

}
