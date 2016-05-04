package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class Computation extends Instruction {

	public Computation(CodeAttribute method) {
		super(method);
	}
	
	public abstract int getOpcode();

	public abstract String getOperator();
	
	public String getReadableDescription() {
		
		return getOperator();
		
	}

	public String getAssociatedName() { return null; }

}
