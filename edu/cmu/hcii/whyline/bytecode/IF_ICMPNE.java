package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IF_ICMPNE extends CompareIntegersBranch {

	public IF_ICMPNE(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 160; }
	public int byteLength() { return 3; }
	
	public String getReadableDescription() { return "!="; }

}
