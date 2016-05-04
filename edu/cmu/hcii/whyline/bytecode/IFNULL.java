package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IFNULL extends CompareToNullBranch {

	public IFNULL(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 198; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public String getReadableDescription() { return "== null"; }

}
