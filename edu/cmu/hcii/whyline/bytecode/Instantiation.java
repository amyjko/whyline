package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class Instantiation extends Instruction {

	public Instantiation(CodeAttribute method) {
		super(method);
	}

	public String getReadableDescription() { return "new"; }

	public String getAssociatedName() { return null; }

	public abstract QualifiedClassName getClassnameOfTypeProduced();
	
}