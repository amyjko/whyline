package edu.cmu.hcii.whyline.trace;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public enum OperandStackType {

	INT(false),			// boolean, byte, char, short, int
	FLOAT(false),		// float
	REFERENCE(false),	// Object
	LONG(true),		// long
	DOUBLE(true)		// double
	
	;
	
	private final boolean isDoubleOrLong;
	
	private OperandStackType(boolean isDoubleOrLong) {
		
		this.isDoubleOrLong = isDoubleOrLong;
		
	}

	public boolean isDoubleOrLong() { return isDoubleOrLong; }
	
}
