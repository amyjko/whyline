package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.PUTFIELD;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class PutFieldExpression extends ConsumerExpression<PUTFIELD> {

	public PutFieldExpression(Decompiler decompiler, PUTFIELD consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		return code.getFieldref().getName() + " = ";

	}

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }
	
	protected Token parseHelper(List<Token> tokens) {

		parseArgument(tokens, 0);
		// Parse until the assignment
		while(tokens.size() > 0 && tokens.get(0).kind != JavaParserConstants.ASSIGN)
			parseThis(tokens);
		// Parse the assignment.
		parseThis(tokens);
		return parseArgument(tokens, 1);
		
	}

}
