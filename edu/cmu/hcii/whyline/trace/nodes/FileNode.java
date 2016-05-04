package edu.cmu.hcii.whyline.trace.nodes;

import edu.cmu.hcii.whyline.source.FileInterface;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class FileNode extends StaticNode<ClassNode> implements Comparable<FileNode> {
	
	public final FileInterface file;
	
	public FileNode(FileInterface file) {

		this.file = file;
		
	}
	
	public String toString() { return file.getShortFileName(); }

	public int compareTo(FileNode o) { return toString().compareTo(o.toString()); }

	public boolean isLeaf() { return false; }

	protected void determineChildren() {}

}