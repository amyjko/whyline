package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DSTORE_0 extends SetLocal {

	public DSTORE_0(CodeAttribute method) {
		super(method);
	}

	public int getLocalID() { return 0; }

	public final int getOpcode() { return 71; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "D"; }

}
