package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IF_ICMPLT extends CompareIntegersBranch {

	public IF_ICMPLT(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 161; }
	public int byteLength() { return 3; }

	public String getReadableDescription() { return "<"; }

}
