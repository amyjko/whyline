package edu.cmu.hcii.whyline.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawImageWithLocationSizeColorAndObserverEvent extends DrawImageEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "image", "x", "y", "width", "height", "color", "observer" }; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	public DrawImageWithLocationSizeColorAndObserverEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paintWithImage(Graphics2D g, Image image) {

		if(image == null)
			drawPlaceholder(g, "image " + getImageID(), getX(), getY(), getWidth(), getHeight(), getColor());
		else
			g.drawImage(image, getX(), getY(), getWidth(), getHeight(), getColor(), null);		
		
	}
	
	protected Shape makeShape() {
		
		return new Rectangle(paintState.getOriginX() + getX(), paintState.getOriginY() + getY(), getWidth(), getHeight());
		
	}

	public void transformContextToDrawPrimitive(Graphics2D g) {
		
		Image image = getGraphicsState().getWindowState().getImage(getImageID());
		g.scale((double)getWidth() / image.getWidth(null), (double)getHeight() / image.getHeight(null));
		g.translate(paintState.getOriginX() + getX(), paintState.getOriginY() + getY());
		
	}

	public int getWindowX() { return paintState.getOriginX() + getX(); }
	public int getWindowY() { return paintState.getOriginY() + getY(); }

	public int getX() { return getInteger(2); }
	public int getY() { return getInteger(3); }
	public int getWidth() { return getInteger(4); }
	public int getHeight() { return getInteger(5); }
	
	public Color getColor() { 
		try { return (Color)getArgument(6).getImmutable(); }
		catch(NoValueException e) { return null; }
	}

	public long getObserver() { return getLong(7); }

	
	public String getHumanReadableName() { return "scaled image"; }

	public String toString() { return super.toString() + "\tdrawImageWithLocationSizeColorAndObserver " + getImage() + " " + getX() + " " + getY() + " " + getObserver(); }

}
