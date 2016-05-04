package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.GetArrayValue;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class GetArrayValueExpression extends Expression<GetArrayValue> {

	public GetArrayValueExpression(Decompiler decompiler, GetArrayValue code) {

		super(decompiler, code);
	
	}

	public String getJavaName() { return "[]"; }

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		parseArgument(tokens, 0);		
		parseThis(tokens);
		parseArgument(tokens, 1);		
		return parseThis(tokens);
		
	}

}
