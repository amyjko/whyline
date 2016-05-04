package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */
public final class ALOAD_2 extends GetLocal {

	public ALOAD_2(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 2; }

	public final int getOpcode() { return 44; }
	public int byteLength() { return 1; }

	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }

}
