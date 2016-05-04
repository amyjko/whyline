package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Andrew J. Ko
 *
 */
import edu.cmu.hcii.whyline.trace.EventKind;

public final class BIPUSH extends PushConstant<Integer> {

	private int value;
	
	public BIPUSH(CodeAttribute method, byte value) {
		super(method);
		this.value = value;
	}

	public Integer getConstant() { return value; }

	public final int getOpcode() { return 16; }
	public int byteLength() { return 2; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(value);
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_BYTE_PRODUCED; }

}
