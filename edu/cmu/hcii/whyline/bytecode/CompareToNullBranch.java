package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class CompareToNullBranch extends ConditionalBranch {

	public CompareToNullBranch(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public String getTypeDescriptorOfArgument(int argIndex) { return "java/lang/Object;"; }

}
