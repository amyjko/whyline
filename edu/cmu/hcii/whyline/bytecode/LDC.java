package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LDC extends PushConstant<Object> {

	private ConstantPoolInfo info;
	
	public LDC(CodeAttribute method, ConstantPoolInfo info) {
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

	public final int getOpcode() { return 18; }
	public int byteLength() { return 2; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(info.getIndexInConstantPool());
		
	}

	public EventKind getTypeProduced() { 
		
		if(info instanceof IntegerInfo) return EventKind.CONSTANT_INTEGER_PRODUCED;
		else if(info instanceof FloatInfo) return EventKind.CONSTANT_FLOAT_PRODUCED;
		else if(info instanceof ClassInfo) return EventKind.OBJECT_PRODUCED;
		else if(info instanceof StringInfo) return EventKind.OBJECT_PRODUCED;
		else throw new RuntimeException("We don't have a kind of event for " + info);
		
	}

	public String getReadableDescription() { return info.toString(); }

}