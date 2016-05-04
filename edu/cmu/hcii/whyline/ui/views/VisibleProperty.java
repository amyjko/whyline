package edu.cmu.hcii.whyline.ui.views;

/**
 * Represents a single value, used by a View to represent some property. Supports animation.
 * 
 * @author Andrew J. Ko
 *
 */
public class VisibleProperty {

	private double value;					// This is the value we're animating to.
	private double visibleValue;		// This is the value that's currently on-screen.
	private double previousValue;
	private boolean animated = false;
	
	public double get() { return value; }
	public double getVisible() { return visibleValue; }
	
	public VisibleProperty(double value) {
		
		this.value = value;
		this.visibleValue = value;
		this.previousValue = value;
		
	}
	
	public boolean set(double newValue) {

		boolean changed = value != newValue;
		
		previousValue = value;
		value = newValue;
		visibleValue = newValue;
		animated = false;
		
		return changed;
		
	}
	
	public boolean animate(double newValue) {

		boolean changed = value != newValue;
		
		// If we're already animating, leave the visible value unchanged
		if(animated) {

			previousValue = visibleValue;
			value = newValue;
			
		}
		else {
			
			previousValue = value;
			value = newValue;
			visibleValue = previousValue;
			animated = true;

		}

		return changed;
		
	}
		
	// This is a cache of values for this formula
	// double transformedPercentage = 1 / (1 + Math.pow(Math.E, -10 * percentComplete + 5));
	private static float[] eToThePercentComplete = new float[101];
	static {
		for(int percentComplete = 0; percentComplete <= 100; percentComplete++)
			eToThePercentComplete[percentComplete] = (float) (1 / (1 + Math.pow(Math.E, -10 * ((float)percentComplete) * .01 + 5)));
	}
	
	public void update(double percentComplete) {

		if(!animated) return;

		if(percentComplete >= 1.0) {
		
			visibleValue = value;
			previousValue = value;
			animated = false;
		
		}
		else if(visibleValue != value) {
			
			// A function for which domain [0-1] maps to range [0-1] in some interesting way.
//			double transformedPercentage = Math.sqrt(Math.sqrt(percentComplete));
//			double transformedPercentage = 1 / (1 + Math.pow(Math.E, -10 * percentComplete + 5));
			float transformedPercentage = eToThePercentComplete[(int)(percentComplete * 100)];
			
			visibleValue = (float)(previousValue + transformedPercentage * (value - previousValue));
			
		}
		
	}
	
}
