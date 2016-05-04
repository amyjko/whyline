package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IFEQ extends CompareIntegerToZeroBranch {
	
	public IFEQ(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 153; }
	public int byteLength() { return 3; }

	public String getReadableDescription() { return "=="; }

}
