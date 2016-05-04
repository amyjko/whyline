package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DUP_X2 extends Duplication {

	public DUP_X2(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 91; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 1; }

	public boolean insertsDuplicatedOperandBelow() { return true; }

}