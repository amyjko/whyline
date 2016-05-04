package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.Conversion;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class CastExpression extends Expression<Conversion> {

	public CastExpression(Decompiler decompiler, Conversion producer) {

		super(decompiler, producer);
		
	}

	public String getJavaName() { return code.getOperator(); }

	public boolean alwaysAppearsInSource() { return false; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {

		// We don't add this cast because we won't associate it with anything.
		return parseArgument(tokens, 0);		
		
	}

}
