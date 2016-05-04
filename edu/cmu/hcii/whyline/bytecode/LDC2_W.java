package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * @author Andrew J. Ko
 *
 */ 
public final class LDC2_W extends PushConstant<Object> {

	private ConstantPoolInfo info;
	
	public LDC2_W(CodeAttribute method, ConstantPoolInfo info) {
		super(method);
		this.info = info;
	}

	public Object getConstant() { 
		
		if(info instanceof LongInfo) return ((LongInfo)info).getValue();
		else if(info instanceof DoubleInfo) return ((DoubleInfo)info).getValue();
		else return null;
		
	}

	public final int getOpcode() { return 20; }
	public int byteLength() { return 3; }
	
	public ConstantPoolInfo getInfo() { return info; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeShort(info.getIndexInConstantPool());

	}

	public EventKind getTypeProduced() { 
		
		if(info instanceof LongInfo) return EventKind.CONSTANT_LONG_PRODUCED;
		else if(info instanceof DoubleInfo) return EventKind.CONSTANT_DOUBLE_PRODUCED;
		else return null;
		
	}

	public String getReadableDescription() { return info.toString(); }

}