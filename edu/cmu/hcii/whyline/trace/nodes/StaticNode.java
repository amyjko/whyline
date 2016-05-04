package edu.cmu.hcii.whyline.trace.nodes;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public abstract class StaticNode<T> extends Node<T> {

	protected final boolean performUpdate() { return false; }

}
