package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.bytecode.Classfile;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class ClassNode extends StaticNode<MethodNode> implements Comparable<ClassNode> {
	
	public final Classfile classfile;
	
	public ClassNode(Classfile classfile) {

		this.classfile = classfile;
		
	}
	
	public String toString() { return classfile.getSimpleName() + ".class"; }

	public int compareTo(ClassNode o) { return toString().compareTo(o.toString()); }

	public boolean isLeaf() { return classfile.getDeclaredMethods().isEmpty(); }

	protected void determineChildren() {}

}