package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.PUTSTATIC;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class PutStaticExpression extends ConsumerExpression<PUTSTATIC> {

	public PutStaticExpression(Decompiler decompiler, PUTSTATIC consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		return code.getFieldref().getName() + " = ";

	}

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }
	
	protected Token parseHelper(List<Token> tokens) {

		// Parse qualifications.
		while(tokens.size() > 0 && !tokens.get(0).getText().equals(code.getFieldref().getName()))
			parseThis(tokens);
		// Parse the name.
		parseThis(tokens);
		// Parse the assignment
		parseThis(tokens);
		return parseArgument(tokens, 1);
		
	}

}
