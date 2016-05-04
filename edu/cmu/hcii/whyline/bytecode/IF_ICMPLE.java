package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IF_ICMPLE extends CompareIntegersBranch {

	public IF_ICMPLE(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 164; }
	public int byteLength() { return 3; }

	public String getReadableDescription() { return "<="; }

}
