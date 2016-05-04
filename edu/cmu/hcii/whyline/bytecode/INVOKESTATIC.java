package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.analysis.Cancelable;
import edu.cmu.hcii.whyline.analysis.ValueSource;

import edu.cmu.hcii.whyline.trace.Trace;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class INVOKESTATIC extends Invoke {

	public INVOKESTATIC(CodeAttribute method, MethodrefInfo methodInfo) {
		super(method, methodInfo);
		this.methodInfo.setStatic();
	}

	public final int getOpcode() { return 184; }
	public int byteLength() { return 3; }
	public int getNumberOfOperandsConsumed() { return super.getNumberOfOperandsConsumed() - 1; }

	public MethodInfo[] getPreciseMethodsCalled(Trace trace, Cancelable cancelable) { return trace.getMethodsFromReference(this); }

	public void toBytes(DataOutputStream code) throws IOException {

		code.writeByte(getOpcode());
		code.writeShort(methodInfo.getIndexInConstantPool());

	}

	public boolean callsOnThis() { return false; }

	// Anything could call a static method (as long as the compiler decided it had access)
	public boolean couldCallOn(ValueSource typeOfThis, Trace trace) {

		return true;
		
	}

}