package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DUP2_X1 extends Dup2lication {

	public DUP2_X1(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 93; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 2; }
	public int getNumberOfOperandsPeekedAt() { return 2; }

	public boolean insertsDuplicatedOperandBelow() { return true; }

}
