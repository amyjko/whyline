package edu.cmu.hcii.whyline.trace.nodes;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class PackageNode extends StaticNode<FileNode> implements Comparable<PackageNode> {
	
	private final String qualifiedName;
	
	public PackageNode(String qualifiedName) {

		this.qualifiedName = qualifiedName;
		
	}
	
	public String toString() { return qualifiedName; }

	public int compareTo(PackageNode o) { return qualifiedName.contains("default") ? -1 : toString().compareTo(o.toString()); }

	public boolean isLeaf() { return false; }

	protected void determineChildren() {}

}