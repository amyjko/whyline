package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LDC_W extends PushConstant<Object> {

	private ConstantPoolInfo info;

	public LDC_W(CodeAttribute method, ConstantPoolInfo info) {
		super(method);
		this.info = info;
	}

	public Object getConstant() { 
		
		if(info instanceof IntegerInfo) return ((IntegerInfo)info).getValue();
		else if(info instanceof FloatInfo) return ((FloatInfo)info).getValue();
		else if(info instanceof ClassInfo) return ((ClassInfo)info).getName();
		else if(info instanceof StringInfo) return ((StringInfo)info).getString();
		else return null;
		
	}

	public final int getOpcode() { return 19; }
	public int byteLength() { return 3; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(info.getIndexInConstantPool());
		
	}

	public EventKind getTypeProduced() { 

		if(info instanceof IntegerInfo) return EventKind.CONSTANT_INTEGER_PRODUCED;
		else if(info instanceof FloatInfo) return EventKind.CONSTANT_FLOAT_PRODUCED;
		else if(info instanceof ClassInfo) return EventKind.OBJECT_PRODUCED;
		else if(info instanceof StringInfo) return EventKind.OBJECT_PRODUCED;
		else throw new RuntimeException("Don't know how to get the type produced for a constant of type " + info.getClass());
		
	}

	public String getReadableDescription() { return info.toString(); }

}