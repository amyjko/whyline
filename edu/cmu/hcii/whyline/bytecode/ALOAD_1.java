package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */
public final class ALOAD_1 extends GetLocal {

	public ALOAD_1(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 1; }

	public final int getOpcode() { return 43; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }

}
