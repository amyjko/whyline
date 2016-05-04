package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ILOAD_2 extends GetLocal {

	public ILOAD_2(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 2; }

	public final int getOpcode() { return 28; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
