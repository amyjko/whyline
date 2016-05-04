package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DLOAD_3 extends GetLocal {

	public DLOAD_3(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 3; }

	public final int getOpcode() { return 41; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.DOUBLE_PRODUCED; }

}
