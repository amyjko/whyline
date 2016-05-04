package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.SetArrayValue;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class SetArrayExpression extends ConsumerExpression<SetArrayValue> {

	public SetArrayExpression(Decompiler decompiler, SetArrayValue consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		return "[] =";

	}

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }
	
	protected Token parseHelper(List<Token> tokens) {

		parseArgument(tokens, 0);
		parseThis(tokens);
		parseArgument(tokens, 1);
		return parseArgument(tokens, 2);
		
	}

}
