package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IFGE extends CompareIntegerToZeroBranch {

	public IFGE(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 156; }
	public int byteLength() { return 3; }

	public String getReadableDescription() { return ">="; }

}
