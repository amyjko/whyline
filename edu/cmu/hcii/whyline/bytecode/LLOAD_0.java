package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LLOAD_0 extends GetLocal {

	public LLOAD_0(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 0; }

	public final int getOpcode() { return 30; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.LONG_PRODUCED; }

}
