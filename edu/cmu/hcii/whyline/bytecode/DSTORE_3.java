package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DSTORE_3 extends SetLocal {

	public DSTORE_3(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 3; }

	public final int getOpcode() { return 74; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "D"; }

}
