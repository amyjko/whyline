package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DUP2 extends Dup2lication {

	public DUP2(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 92; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 2; }
	public int getNumberOfOperandsPeekedAt() { return 2; }

}
