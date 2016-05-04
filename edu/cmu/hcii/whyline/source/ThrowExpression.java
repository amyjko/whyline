package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.ATHROW;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ThrowExpression extends ConsumerExpression<ATHROW> {

	public ThrowExpression(Decompiler decompiler, ATHROW consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		return "throw";

	}

	// Sometimes synchronization blocks throw exceptions if the lock object is null. These don't appear in source.
	public boolean alwaysAppearsInSource() { return false; }
	public boolean mayAppearInSource() { return true; }
	
	protected Token parseHelper(List<Token> tokens) {

		return parseArgument(tokens, 0);
		
	}

}
