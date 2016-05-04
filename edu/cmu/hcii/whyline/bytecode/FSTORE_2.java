package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class FSTORE_2 extends SetLocal {

	public FSTORE_2(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 2; }

	public final int getOpcode() { return 69; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "F"; }

}
