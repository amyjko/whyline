package edu.cmu.hcii.whyline.io;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import edu.cmu.hcii.whyline.bytecode.Invoke;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.UI;


// Represents:
//
// drawGlyphVector(GlyphVector g, float x, float y)
// drawString(AttributedCharacterIterator iterator, int x, int y) 
// drawString(AttributedCharacterIterator iterator, float x, float y)
//
// AttributedCharacterIterator is an interface, implemented by...
// 	javax.swing.text.TextLayoutStrategy$AttributedSegment (static)
//		java.text.AttributedString$AttributedStringIterator (private)

/**
 * @author Andrew J. Ko
 *
 */
public final class DrawCharacterSequenceEvent extends RenderEvent {

	private static final String[] ARGUMENT_NAMES = { "graphics", "iterator", "x", "y"}; 
	public String getArgumentName(int index) { return ARGUMENT_NAMES[index]; }

	private Graphics2D renderContext;
	private final boolean usesFloats;
	
	private static final String UNKNOWN_TEXT = "unknown text";
	
	public DrawCharacterSequenceEvent(Trace trace, int eventID) {
		
		super(trace, eventID);

		String descriptor = ((Invoke)trace.getInstruction(eventID)).getMethodInvoked().getMethodDescriptor();

		if(descriptor.equals("(Ljava/text/AttributedCharacterIterator;FF)V") || descriptor.equals("(Ljava/awt/font/GlyphVector;FF)V"))
			usesFloats = true;
		else 
			usesFloats = false;
		
	}
	
	public void paint(Graphics2D g) {

		if(renderContext == null) renderContext = g;

		g = (Graphics2D)g.create();
		
		g.setColor(UI.getHighlightColor());
		g.drawString(UNKNOWN_TEXT, getX(), getY());
		
	}

	protected Shape makeShape() {

		if(renderContext == null) return null;
		SetFontEvent setFont = paintState == null ? null : paintState.getLatestFontChange();
		Font font = setFont == null ? UI.getFixedFont() : setFont.getFont();
		FontMetrics metrics = renderContext.getFontMetrics(font);
		Rectangle2D r = metrics.getStringBounds(UNKNOWN_TEXT, renderContext);
		int ascent = metrics.getAscent();
		
		return new Rectangle((getX() + paintState.getOriginX()), (getY() - ascent + paintState.getOriginY()), (int)r.getWidth(), (int)r.getHeight());
//		return glyphs.getOutline(getX() + paintState.getOriginX(), getY() + paintState.getOriginY());
		
	}

	public int getX() { return usesFloats ? (int)getFloat(2) : getInteger(2); }
	public int getY() { return usesFloats ? (int)getFloat(3) : getInteger(3); }

	public String getHumanReadableName() { return "text"; }

	public boolean canOcclude() { return false; }

	public String toString() { return super.toString() + getGraphicsID() + "\tdrawString <iterator> " + getX() + " " + getY(); }

}
