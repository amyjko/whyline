package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IF_ICMPGE extends CompareIntegersBranch {

	public IF_ICMPGE(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 162; }
	public int byteLength() { return 3; }

	public String getReadableDescription() { return ">="; }

}
