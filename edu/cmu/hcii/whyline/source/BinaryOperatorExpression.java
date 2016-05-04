package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.BinaryComputation;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class BinaryOperatorExpression extends Expression<BinaryComputation> {

	public BinaryOperatorExpression(Decompiler decompiler, BinaryComputation comp) {
		
		super(decompiler, comp);
		
	}

	public String getJavaName() { return code.getOperator(); }
	
	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		parseArgument(tokens, 0);		
		parseThis(tokens);
		return parseArgument(tokens, 1);
		
	}

}
