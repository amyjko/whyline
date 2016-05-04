package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class POP2 extends StackManipulation {

	public POP2(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 88; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public EventKind getTypeProduced() { return null; }

	public String getReadableDescription() { return "pop2"; }

}
