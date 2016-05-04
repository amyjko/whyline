package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.PushConstant;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class LiteralExpression extends Expression<PushConstant<?>> {

	public LiteralExpression(Decompiler decompiler, PushConstant<?> constant) {
		
		super(decompiler, constant);
		
	}
	
	public String getJavaName() { 
		
		Object constant = ((PushConstant<?>)code).getConstant();
		return constant instanceof String ? "\"" + constant + "\"" : String.valueOf(constant); 
		
	}

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		return parseThis(tokens);
		
	}

}
