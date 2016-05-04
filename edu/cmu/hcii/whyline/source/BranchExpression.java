package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.Branch;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class BranchExpression extends ConsumerExpression<Branch> {

	public BranchExpression(Decompiler decompiler, Branch consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		return code.getReadableDescription();

	}

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }

	protected Token parseHelper(List<Token> tokens) {

		parseArgument(tokens, 0);
		Token last = parseThis(tokens);
		if(code.getNumberOfOperandsConsumed() > 1)
			return parseArgument(tokens, 1);
		else
			return last;
		
	}
	
}
