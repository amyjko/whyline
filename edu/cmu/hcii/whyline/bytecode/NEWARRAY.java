package edu.cmu.hcii.whyline.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.cmu.hcii.whyline.trace.EventKind;

/**
 * Creates primitive valued arrays.
 * 
 * @author Andrew J. Ko
 *
 */ 
public final class NEWARRAY extends ArrayAllocation {

	private int type;
	
	public NEWARRAY(CodeAttribute method, int type) {
		super(method);
		this.type = type;
	}

	public final int getOpcode() { return 188; }
	public int byteLength() { return 2; }
	public int getNumberOfOperandsConsumed() { return 1; }
	public int getNumberOfOperandsProduced() { return 1; }
	public int getNumberOfOperandsPeekedAt() { return 0; }

	public void toBytes(DataOutputStream code) throws IOException {
		
		code.writeByte(getOpcode());
		code.writeByte(type);
		
	}

	public static final String BYTE = "B";
	public static final String CHAR = "C";
	public static final String DOUBLE = "D";
	public static final String FLOAT = "F";
	public static final String INT = "I";
	public static final String LONG = "J";
	public static final String SHORT = "S";
	public static final String BOOLEAN = "Z";
	public static final String VOID = "V";

	public char getTypeDescriptorCharacter() {
		
		switch(type) {
		case 4 : return 'Z';
		case 5 : return 'C';
		case 6 : return 'F';
		case 7 : return 'D';
		case 8 : return 'B';
		case 9 : return 'S';
		case 10 : return 'I';
		case 11 : return 'J';
		default : throw new RuntimeException("The impossible has occurred: an illegal NEWARRAY type value: " + type);
		}
		
	}
	
	public EventKind getTypeProduced() { return EventKind.OBJECT_PRODUCED; }

	public String getTypeDescriptorOfArgument(int argIndex) { return "I"; }

	public QualifiedClassName getClassnameOfTypeProduced() {
		
		return QualifiedClassName.get("[" + getTypeDescriptorCharacter());
		
	}
	
}