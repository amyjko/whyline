package edu.cmu.hcii.whyline.trace.nodes;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class DynamicNode<T> extends Node<T> {

	public abstract int getAssociatedEventID();
	public abstract String getAssociatedEventIDDescription();

}
