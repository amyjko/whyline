package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IFLT extends CompareIntegerToZeroBranch {

	public IFLT(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 155; }
	public int byteLength() { return 3; }

	public String getReadableDescription() { return "<"; }

}
