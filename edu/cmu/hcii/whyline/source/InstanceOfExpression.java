package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.INSTANCEOF;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class InstanceOfExpression extends Expression<INSTANCEOF> {

	public InstanceOfExpression(Decompiler decompiler, INSTANCEOF producer) {

		super(decompiler, producer);
		
	}

	public String getJavaName() { return "instanceof"; }

	public boolean alwaysAppearsInSource() { return false; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		parseThis(tokens);
		return parseArgument(tokens, 0);		
		
	}

}
