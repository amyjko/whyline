package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class CompareReferencesBranch extends ConditionalBranch {

	public CompareReferencesBranch(CodeAttribute method, int offset) {
		super(method, offset);
	}

	public String getTypeDescriptorOfArgument(int argIndex) { return "Ljava/lang/Object;"; }

}
