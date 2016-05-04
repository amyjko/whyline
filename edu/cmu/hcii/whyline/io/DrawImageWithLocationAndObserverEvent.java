package edu.cmu.hcii.whyline.io;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;

import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawImageWithLocationAndObserverEvent extends DrawImageEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "image", "x", "y", "observer" }; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	public DrawImageWithLocationAndObserverEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

	}

	public void paintWithImage(Graphics2D g, Image image) {

		if(image == null) {
			ImageData data = getTrace().getImageData(getImageID());
			if(data != null)
				drawPlaceholder(g, "image " + getImageID(), getX(), getY(), data.getWidth(eventID), data.getHeight(eventID), null);
		}
		else
			g.drawImage(image, getX(), getY(), null);		

	}

	protected Shape makeShape() {
		
		Image image = getGraphicsState().getWindowState().getImage(getImageID());
		ImageData data = getTrace().getImageData(getImageID());
		int width = data == null ? 25 : data.getWidth(eventID);
		int height = data == null ? 25 : data.getHeight(eventID);
		return new Rectangle(paintState.getOriginX() + getX(), paintState.getOriginY() + getY(), width, height);
		
	}

	public void transformContextToDrawPrimitive(Graphics2D g) {
		
		g.translate(paintState.getOriginX() + getX(), paintState.getOriginY() + getY());
		
	}

	public int getWindowX() { return paintState.getOriginX() + getX(); }
	public int getWindowY() { return paintState.getOriginY() + getY(); }

	public int getX() { return getInteger(2); }
	public int getY() { return getInteger(3); }
	public long getObserver() { return getLong(4); }
	
	public String getHumanReadableName() { return "image"; }

	public String toString() { return super.toString() + "\tdrawImageWithLocationAndObserver " + getImage() + " " + getX() + " " + getY(); }

}
