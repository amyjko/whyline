package edu.cmu.hcii.whyline.bytecode;


import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public abstract class Definition extends Instruction {

	public Definition(CodeAttribute method) {
		super(method);
	}

	public abstract int getOpcode();

	public abstract String getLocalIDName();
	
	public String getReadableDescription() { return getLocalIDName() + "="; }

	public final EventKind getTypeProduced() { return null; }
	
}
