package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LASTORE extends SetArrayValue {

	public LASTORE(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 80; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArray() { return "L"; }

}
