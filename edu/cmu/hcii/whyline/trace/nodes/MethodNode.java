package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class MethodNode extends StaticNode<Object> implements Comparable<MethodNode> {
	
	public final MethodInfo method;
	
	public MethodNode(MethodInfo method) {

		this.method = method;
		
	}
	
	public String toString() { return method.getJavaName() + "()"; }

	public int compareTo(MethodNode o) { return toString().compareTo(o.toString()); }

	public boolean isLeaf() { return true; }

	protected void determineChildren() {}

}