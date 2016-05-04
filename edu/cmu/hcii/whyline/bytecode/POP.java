package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class POP extends StackManipulation {

	public POP(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 87; }
	public int byteLength() { return 1; }
	public final int getNumberOfOperandsConsumed() { return 0; }
	public final int getNumberOfOperandsProduced() { return 0; }
	public final int getNumberOfOperandsPeekedAt() { return 0; }

	public EventKind getTypeProduced() { return null; }

	public String getReadableDescription() { return "pop"; }

}
