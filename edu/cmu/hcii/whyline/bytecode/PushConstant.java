package edu.cmu.hcii.whyline.bytecode;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class PushConstant<T> extends Instruction {

	public PushConstant(CodeAttribute method) {
		super(method);
	}

	public abstract T getConstant();
	
	public final int getNumberOfOperandsConsumed() { return 0; }
	public final int getNumberOfOperandsProduced() { return 1; }
	public final int getNumberOfOperandsPeekedAt() { return 0; }

	public String getTypeDescriptorOfArgument(int argIndex) { return null; }

	public String getReadableDescription() { return String.valueOf(getConstant()); }

	public String getAssociatedName() { return null; }

	public String toString() { return super.toString() + " " + getConstant(); }

}