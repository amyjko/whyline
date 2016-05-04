package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IF_ICMPEQ extends CompareIntegersBranch {
	
	public IF_ICMPEQ(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 159; }
	public int byteLength() { return 3; }
	
	public String getReadableDescription() { return "=="; }

}

