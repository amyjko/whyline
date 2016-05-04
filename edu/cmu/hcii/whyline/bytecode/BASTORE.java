package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */
public final class BASTORE extends SetArrayValue {

	public BASTORE(CodeAttribute method) {
		super(method);
	}

	public final int getOpcode() { return 84; }
	public int byteLength() { return 1; }

	public String getTypeDescriptorOfArray() { return "B"; }

}
