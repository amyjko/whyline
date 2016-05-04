package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ILOAD_0 extends GetLocal {

	public ILOAD_0(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 0; }
	
	public final int getOpcode() { return 26; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.CONSTANT_INTEGER_PRODUCED; }

}
