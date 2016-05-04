package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IFNE extends CompareIntegerToZeroBranch {

	public IFNE(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 154; }
	public int byteLength() { return 3; }
	
	public String getReadableDescription() { return "!="; }

}
