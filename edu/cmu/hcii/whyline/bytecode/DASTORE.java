package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class DASTORE extends SetArrayValue {

	public DASTORE(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 82; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArray() { return "D"; }

}
