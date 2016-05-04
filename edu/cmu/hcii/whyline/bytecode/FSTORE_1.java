package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class FSTORE_1 extends SetLocal {

	public FSTORE_1(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 1; }

	public final int getOpcode() { return 68; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "F"; }

}
