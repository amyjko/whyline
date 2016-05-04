package edu.cmu.hcii.whyline.bytecode;



/**
 * @author Andrew J. Ko
 *
 */
public abstract class ArrayAllocation extends Instantiation {

	public ArrayAllocation(CodeAttribute method) {
		super(method);
	}
	
	public String getReadableDescription() { return "new"; }

	public String getAssociatedName() { return null; }

}
