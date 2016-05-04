package edu.cmu.hcii.whyline.bytecode;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class Use extends Instruction {
	
	public Use(CodeAttribute method) {
		super(method);
	}

	public abstract int getOpcode();
	
}