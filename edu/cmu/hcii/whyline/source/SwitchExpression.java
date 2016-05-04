package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.TableBranch;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class SwitchExpression extends ConsumerExpression<TableBranch> {

	public SwitchExpression(Decompiler decompiler, TableBranch consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		return "switch";

	}

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }
	
	protected Token parseHelper(List<Token> tokens) {

		return parseArgument(tokens, 0);
		
	}

}
