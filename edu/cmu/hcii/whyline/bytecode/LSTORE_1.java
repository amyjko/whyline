package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LSTORE_1 extends SetLocal {

	public LSTORE_1(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 1; }

	public final int getOpcode() { return 64; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "L"; }

}
