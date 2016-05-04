package edu.cmu.hcii.whyline.source;

import java.util.List;

import edu.cmu.hcii.whyline.bytecode.MONITORENTER;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class MonitorEnterExpression extends ConsumerExpression<MONITORENTER> {

	public MonitorEnterExpression(Decompiler decompiler, MONITORENTER consumer) {

		super(decompiler, consumer);
	
	}

	public String getJavaName() {
		
		return "synchronized(x) { ...";

	}

	public boolean alwaysAppearsInSource() { return true; }
	public boolean mayAppearInSource() { return true; }
	
	protected Token parseHelper(List<Token> tokens) {
		
		return parseArgument(tokens, 0);		
		
	}

}
