package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.ARRAYLENGTH;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ArrayLengthExpression extends Expression<ARRAYLENGTH> {

	public ArrayLengthExpression(Decompiler decompiler, ARRAYLENGTH arraylength) {
		
		super(decompiler, arraylength);
		
	}

	public String getJavaName() { return "length"; }

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		parseArgument(tokens, 0);
		return parseThis(tokens);
		
	}

}
