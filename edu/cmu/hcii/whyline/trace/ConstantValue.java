package edu.cmu.hcii.whyline.trace;

import edu.cmu.hcii.whyline.bytecode.PUTFIELD;
import edu.cmu.hcii.whyline.bytecode.PushConstant;
import edu.cmu.hcii.whyline.qa.*;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class ConstantValue extends Value {

	private final PushConstant<?> instruction;
	
	public ConstantValue(Trace trace, PushConstant<?> instruction) {
		
		super(trace);
		
		this.instruction = instruction;
		
	}

	public PushConstant<?> getInstruction() { return instruction; }
	
	public boolean getBoolean() { return (Boolean)getValue(); }
	public int getInteger() { return (Integer)getValue(); } 
	public float getFloat() { return (Float)getValue(); }
	public double getDouble() { return (Double)getValue(); }

	public Object getValue() { return instruction.getConstant(); }

	public Object getImmutable() { return getValue(); }

	public boolean isObject() { return getValue() == null || getValue() instanceof String; }
	
	public long getLong() {

		Object obj = getValue();
		
		if(obj == null) return 0;
		else if(obj instanceof String) return trace.getIDOfImmutable(obj);
		else throw new RuntimeException("This constant cannot be converted to long; conversion is only for constant objects.");
		
	}
	
	public String getVerbalExplanation() { 

		String valueText = "<b>" + getDisplayName(true) + "</b>";

		return valueText +" was a constant";
			
	}
	
	public String getDisplayName(boolean html) { 
		
		Object value = getValue();
		
		if(instruction.getConsumers().getFirstConsumer() instanceof PUTFIELD) {
			if(((PUTFIELD)instruction.getConsumers().getFirstConsumer()).getFieldref().getTypeDescriptor().equals("Z"))
				return ((Integer)value) == 0 ? "false" : "true";
		}
		
		return "" + value;
		
	}

	public boolean hasEventID() { return false; }

	public int getEventID() { return -1; }

	public Answer getAnswer(Question<?> q) {
		
		return new ConstantValueAnswer(q, this);
		
	}

	public String toString() { return "ConstantStackValue: " + instruction; }

}