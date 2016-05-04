package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.AbstractReturn;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ReturnExpression extends ConsumerExpression<AbstractReturn> {

	public ReturnExpression(Decompiler decompiler, AbstractReturn consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		return "return";

	}

	public boolean alwaysAppearsInSource() { return false; }
	public boolean mayAppearInSource() { return true; }
	
	protected Token parseHelper(List<Token> tokens) {

		return parseArgument(tokens, 0);
		
	}

}
