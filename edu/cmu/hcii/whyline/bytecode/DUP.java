package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DUP extends Duplication {

	public DUP(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 89; }
	public int byteLength() { return 1; }
	public int getNumberOfOperandsConsumed() { return 0; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 1; }
		
}