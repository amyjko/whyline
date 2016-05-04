package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class CompareIntegersBranch extends ConditionalBranch {

	public CompareIntegersBranch(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public int getNumberOfOperandsConsumed() { return 2; }
	public int getNumberOfOperandsProduced() { return 0; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
