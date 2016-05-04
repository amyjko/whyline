package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IF_ICMPGT extends CompareIntegersBranch {

	public IF_ICMPGT(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 163; }
	public int byteLength() { return 3; }

	public String getReadableDescription() { return ">"; }

}
