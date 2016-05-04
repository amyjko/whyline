package edu.cmu.hcii.whyline.trace.nodes;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class Node<T>  {

	private boolean childrenDetermined = false;
	private final SortedSet<T> children = new TreeSet<T>();
	
	protected boolean isUpdated  = false;

	public Node() {}		
	
	public T get(int index) {
		
		int i = 0;
		for(T child : getChildren()) {
			if(i == index) return child;
			else i++;
		}
		return null;
		
	}

	public int indexOf(Object c) {
		
		int i = 0;
		for(T child : getChildren()) {
			if(child == c) return i;
			i++;
		}
		return -1;
		
	}

	protected boolean hasDeterminedChildren() { return childrenDetermined; }
	
	public  SortedSet<T> getChildren() { 
	
		if(!childrenDetermined) {
			determineChildren();
			childrenDetermined = true;
		}
		return children; 
		
	}
	
	public void resetChildren() { 
	
		children.clear();
		determineChildren(); 
		
	}

	public int getNumberOfChildren() { return getChildren().size(); }

	protected abstract void determineChildren();
	
	public void addChild(T child) { children.add(child); }

	public abstract boolean isLeaf();

	public abstract String toString();

	public void removeChild(T child) { children.remove(child); }

	public final boolean update() {
		
		if(!isUpdated) {
			boolean changed = performUpdate();
			isUpdated = true;
			return changed;
		}
		else return false;		
		
	}
	
	protected abstract boolean performUpdate();
	
}