package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.UnaryComputation;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class UnaryOperatorExpression extends Expression<UnaryComputation> {

	public UnaryOperatorExpression(Decompiler decompiler, UnaryComputation code) {
		
		super(decompiler, code);
		
	}

	public String getJavaName() { return code.getOperator(); }

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		parseThis(tokens);
		return parseArgument(tokens, 0);		
				
	}

}
