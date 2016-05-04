package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class StackManipulation extends Instruction {

	public StackManipulation(CodeAttribute method) {
		super(method);
	}
	
	public abstract int getOpcode();

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public String getAssociatedName() { return null; }

	public String getTypeDescriptorOfArgument(int argIndex) { return null; }

}
