package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.GETFIELD;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class GetFieldExpression extends Expression<GETFIELD> {

	public GetFieldExpression(Decompiler decompiler, GETFIELD producer) {
		
		super(decompiler, producer);
		
	}

	public String getJavaName() { return code.getFieldref().getName(); }

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		// Keep reading tokens while the current token isn't this field name
		parseArgument(tokens, 0);
		// Now read the field name.
		return parseThis(tokens);
		
	}

}
