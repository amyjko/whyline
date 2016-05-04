package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */
public final class ACONST_NULL extends PushConstant<Object> {

	public ACONST_NULL(CodeAttribute method) {
		super(method);
	}

	public Object getConstant() { return null; }

	public final int getOpcode() { return 1; }
	public int byteLength() { return 1; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		
	}

	public EventKind getTypeProduced() { return EventKind.CONSTANT_OBJECT_PRODUCED; }

}
