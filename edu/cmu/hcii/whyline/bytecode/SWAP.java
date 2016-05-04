package edu.cmu.hcii.whyline.bytecode;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class SWAP extends StackManipulation {

	public SWAP(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 95; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 2; }

	public EventKind getTypeProduced() { return null; }

	public String getReadableDescription() { return "swap"; }

}
