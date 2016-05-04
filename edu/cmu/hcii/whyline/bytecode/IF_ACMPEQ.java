package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class IF_ACMPEQ extends CompareReferencesBranch {
	
	public IF_ACMPEQ(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public final int getOpcode() { return 165; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return 2; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public String getReadableDescription() { return "=="; }

}
