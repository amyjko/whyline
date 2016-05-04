package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.Instantiation;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class NewExpression extends Expression<Instantiation> {

	public NewExpression(Decompiler decompiler, Instantiation allocation) {

		super(decompiler, allocation);
		
	}

	public String getJavaName() { return "new " + code.getClassnameOfTypeProduced().getSimpleName(); }

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {
		
		return parseThis(tokens);
		
	}

}
