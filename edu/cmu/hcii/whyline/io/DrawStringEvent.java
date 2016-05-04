package edu.cmu.hcii.whyline.io;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import edu.cmu.hcii.whyline.bytecode.Invoke;
import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawStringEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "text", "x", "y"}; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	private String string;
	private Graphics2D renderContext;
	private final boolean usesFloats;
	
	public DrawStringEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

		usesFloats = ((Invoke)trace.getInstruction(eventID)).getMethodInvoked().getMethodDescriptor().equals("(Ljava/lang/String;FF)V");

	}
	
	public void paint(Graphics2D g) {
	
		g.drawString(getString(), getX(), getY());
		if(renderContext == null) renderContext = g;

	}

	protected Shape makeShape() {

		if(renderContext == null) return null;

		Font font = paintState.getLatestFontChange().getFont();
		FontMetrics metrics = renderContext.getFontMetrics(font);
		Rectangle2D r = metrics.getStringBounds(getString(), renderContext);
		int ascent = metrics.getAscent();
		
		return font.createGlyphVector(renderContext.getFontRenderContext(), string).getOutline(getX() + paintState.getOriginX(), getY() + paintState.getOriginY()); 
		
	}
	
	public String getString() { 
	
		if(string == null) {
			try { string = (String)getArgument(1).getImmutable(); }
			catch(NoValueException e) {}
		}
		return string;
		
	}
	
	public int getX() { return usesFloats ? (int)getFloat(2) : getInteger(2); }
	public int getY() { return usesFloats ? (int)getFloat(3) : getInteger(3); }

	public String getHumanReadableName() { return "text"; }

	public boolean canOcclude() { return false; }

	public String toString() { return super.toString() + getGraphicsID() + "\tdrawString " + getX() + " " + getY() + " " + getString(); }

}
