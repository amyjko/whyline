package edu.cmu.hcii.whyline.bytecode;

/**
 * @author Andrew J. Ko
 *
 */
public final class AASTORE extends SetArrayValue {

	public AASTORE(CodeAttribute method) {
		super(method);
	}

	public String getTypeDescriptorOfArray() { return "Ljava/lang/Object;"; }

	public final int getOpcode() { return 83; }
	public int byteLength() { return 1; }
	
}
