package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ILOAD_1 extends GetLocal {

	public ILOAD_1(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 1; }

	public final int getOpcode() { return 27; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
