package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class ISTORE_2 extends SetLocal {

	public ISTORE_2(CodeAttribute method) {
		super(method);
	}
	
	public int getLocalID() { return 2; }

	public final int getOpcode() { return 61; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

}
