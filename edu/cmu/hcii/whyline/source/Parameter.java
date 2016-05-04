package edu.cmu.hcii.whyline.source;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class Parameter {

	private final MethodInfo method;
	private final int parameter;

	/**
	 * The parameter starts at 0 (representing "this" if an instance method)
	 */
	public Parameter(MethodInfo method, int parameter) {
		
		this.method = method;
		this.parameter = parameter;
		
	}

	public MethodInfo getMethod() { return method; }

	public int getNumber() { return parameter; }
	
}
