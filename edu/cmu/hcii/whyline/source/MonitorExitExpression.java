package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.MONITOREXIT;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class MonitorExitExpression extends ConsumerExpression<MONITOREXIT> {

	public MonitorExitExpression(Decompiler decompiler, MONITOREXIT consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		return "synch ... }";

	}

	public boolean alwaysAppearsInSource() { return false; }
	public boolean mayAppearInSource() { return false; }
	
	protected Token parseHelper(List<Token> tokens) {
	
		return null;
		
	}

}
